package org.purescript.module.declaration.value.expression.dostmt

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.childrenOfType
import org.purescript.module.declaration.value.ValueNamespace
import org.purescript.module.declaration.value.expression.Expression
import org.purescript.psi.PSPsiElement

class AdoBlock(node: ASTNode) : PSPsiElement(node), ValueNamespace, Expression {
    val statements get() = childrenOfType<DoStatements>().single()
    override val valueNames: Sequence<PsiNamedElement> get() = statements?.valueNames ?: emptySequence()
}