package org.purescript.ide.refactoring

import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parents
import com.intellij.psi.util.parentsOfType
import com.intellij.refactoring.RefactoringBundle.message
import com.intellij.refactoring.introduce.IntroduceHandler
import com.intellij.refactoring.introduce.PsiIntroduceTarget
import com.intellij.refactoring.introduce.inplace.AbstractInplaceIntroducer
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser
import com.intellij.usageView.UsageInfo
import org.purescript.file.PSFileType
import org.purescript.psi.PSPsiFactory
import org.purescript.psi.declaration.classes.PSClassMemberList
import org.purescript.psi.declaration.value.ValueDeclarationGroup
import org.purescript.psi.expression.Expression
import org.purescript.psi.expression.ExpressionSelector
import org.purescript.psi.expression.PSExpressionIdentifier
import org.purescript.psi.module.Module

class ExpressionIdentifierIntroduceHandler :
    IntroduceHandler<PsiIntroduceTarget<Expression>, Module>() {
    /**
     * example:
     * ```purescript
     * f n = if true then show n else show n
     * ```
     * `show n` can be extracted, and then the other `show n` is a usage
     *
     */
    override fun collectUsages(target: PsiIntroduceTarget<Expression>, scope: Module): MutableList<UsageInfo> {
        return mutableListOf()
    }

    override fun checkUsages(usages: MutableList<UsageInfo>) = null

    /**
     * foo = {-caret-}a + b + c
     * targets could be
     *  * a
     *  * a + b
     *  * a + b + c
     */
    override fun collectTargets(file: PsiFile, editor: Editor, project: Project)
            : Pair<MutableList<PsiIntroduceTarget<Expression>>, Int> {
        val offset = editor.caretModel.offset
        val psiUnderCursor = psiNextToOffset(file, offset)
            ?: return Pair.create(mutableListOf(), 0)
        val selector = ExpressionSelector()
        val expressions = selector.getNonFilteredExpressions(psiUnderCursor, editor.document, editor.caretModel.offset)
        val targets = expressions.map { PsiIntroduceTarget(it) }.toMutableList()
        return Pair.create(targets, 0) // 0 selected target
    }

    private fun psiNextToOffset(file: PsiFile, offset: Int) =
        psiAtOffset(file, offset) ?: psiAtOffset(file, offset - 1)

    override fun findSelectionTarget(start: Int, end: Int, file: PsiFile, editor: Editor, project: Project)
            : PsiIntroduceTarget<Expression>? {
        val startElement = file.findElementAt(start) ?: return null
        val endElement = file.findElementAt(end - 1) ?: return null
        val commonElement = startElement.parents(true).firstOrNull() { it.textRange.contains(endElement.textRange) }
        return commonElement
            ?.parentsOfType<Expression>()
            ?.firstOrNull()
            ?.let { PsiIntroduceTarget(it) }
            ?.takeIf { checkSelectedTarget(it, file, editor, project) == null }
    }

    private fun psiAtOffset(file: PsiFile, offset: Int) =
        file.findElementAt(offset)?.parentsOfType<Expression>()?.firstOrNull()

    override fun getRefactoringName() = message("extract.method.title")
    override fun getHelpID() = null
    override fun getChooseScopeTitle() = "Choose scope <title>"
    override fun getScopeRenderer() = DefaultPsiElementCellRenderer() as PsiElementListCellRenderer<Module>
    override fun checkSelectedTarget(t: PsiIntroduceTarget<Expression>, f: PsiFile, e: Editor, p: Project): String? {
        return null
        val atoms = t.place?.getAtoms()?.filterIsInstance<PSExpressionIdentifier>()
            ?: return "Empty target"
        return atoms.firstNotNullOfOrNull {
            when (it.reference.resolve()?.parent) {
                is Module -> null
                is PSClassMemberList -> null
                else -> "'${it.name}' cant be reached from top level"
            }
        }
    }
    
    fun getParameters(expt: Expression): Sequence<PSExpressionIdentifier> {
        return expt.getAtoms().filterIsInstance<PSExpressionIdentifier>().filter {
            when (it.reference.resolve()?.parent) {
                is Module , is PSClassMemberList -> false
                else -> true
            }
        }
    }


    override fun collectTargetScopes(t: PsiIntroduceTarget<Expression>, e: Editor, f: PsiFile, p: Project)
            : MutableList<Module> = t.place?.module?.let { mutableListOf(it) } ?: mutableListOf()

    override fun getIntroducer(
        target: PsiIntroduceTarget<Expression>,
        scope: Module,
        usages: MutableList<UsageInfo>,
        replaceChoice: OccurrencesChooser.ReplaceChoice,
        file: PsiFile,
        editor: Editor,
        project: Project
    ): AbstractInplaceIntroducer<ValueDeclarationGroup, Expression> {
        val factory = project.service<PSPsiFactory>()
        val occurrences = usages.map { it.element as Expression }.toTypedArray()
        val psi = target.place ?: error("Empty target")
        val expr = psi.text ?: error("Could not extract text form expression")
        val name = (psi.getAtoms()?.filterIsInstance<PSExpressionIdentifier>()?.firstOrNull()?.name
            ?: "expr") + "'"
        val parameters = getParameters(psi).toList()
        val nameWithParameters = "$name ${parameters.joinToString(" ") { it.name }}"
        return object : AbstractInplaceIntroducer<ValueDeclarationGroup, Expression>(
            project,
            editor,
            target.place,
            null,
            occurrences,
            message("extract.method.title"),
            PSFileType
        ) {
            /* this should be the ID of the shortcut action,
            * not sure if and to what it is used */
            override fun getActionName() = "ExtractMethod"

            /*
            we currently don't have a settings balloon dialog and also have
            no settings to save
            */
            override fun getComponent() = null
            override fun saveSettings(variable: ValueDeclarationGroup) = Unit

            override fun setReplaceAllOccurrences(value: Boolean) = Unit

            // this seems to never be used
            override fun performIntroduce() = Unit

            /**
             * Unsure how this is supposed to work, but we return a un attached
             * version of the template
             */
            override fun getVariable() = factory.createValueDeclarationGroup(nameWithParameters, expr)
                ?: error("Could not create value declaration")

            /**
             * This inserts the extracted method into the document
             */
            override fun createFieldToStartTemplateOn(replaceAll: Boolean, names: Array<out String>) = runWriteAction {
                for (parameter in parameters) {
                    this.expr?.parent?.addAfter(parameter, this.expr)
                    this.expr?.parent?.addAfter(factory.createSpace(), this.expr)
                }
                scope.add(factory.createNewLines(2))
                scope.addTyped(variable)
            }

            override fun suggestNames(replaceAll: Boolean, variable: ValueDeclarationGroup?): Array<String> = emptyArray()

            override fun isReplaceAllOccurrences() =
                when (replaceChoice) {
                    OccurrencesChooser.ReplaceChoice.NO -> false
                    OccurrencesChooser.ReplaceChoice.NO_WRITE -> false
                    OccurrencesChooser.ReplaceChoice.ALL -> true
                }

            override fun restoreExpression(
                containingFile: PsiFile,
                variable: ValueDeclarationGroup,
                marker: RangeMarker,
                exprText: String?
            ) = psi
        }
    }
}
