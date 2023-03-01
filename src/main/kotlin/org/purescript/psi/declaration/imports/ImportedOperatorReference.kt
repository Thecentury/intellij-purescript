package org.purescript.psi.declaration.imports

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.purescript.psi.PSPsiFactory

class ImportedOperatorReference(element: PSImportedOperator) :
    PsiReferenceBase<PSImportedOperator>(element, element.symbol.operator.textRangeInParent, false) {
    override fun getVariants(): Array<Any> = candidates.toList().toTypedArray()
    override fun resolve(): PsiElement? = candidates.firstOrNull { it.name == element.name }
    val candidates get() = element.importDeclaration.importedModule?.exportedFixityDeclarations ?: emptySequence()
    override fun handleElementRename(name: String): PsiElement? {
        val newName = PSPsiFactory(element.project).createOperatorName(name)
            ?: return null
        element.symbol.operator.replace(newName)
        return element
    }

}
