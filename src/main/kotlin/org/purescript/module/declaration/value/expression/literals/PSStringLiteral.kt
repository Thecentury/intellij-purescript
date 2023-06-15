package org.purescript.module.declaration.value.expression.literals

import com.intellij.lang.ASTNode
import org.purescript.module.declaration.value.Similar
import org.purescript.module.declaration.value.expression.ExpressionAtom
import org.purescript.psi.PSPsiElement
import org.purescript.typechecker.Prim

class PSStringLiteral(node: ASTNode) : PSPsiElement(node), ExpressionAtom {
    override fun areSimilarTo(other: Similar): Boolean =
        other is PSStringLiteral && this.text == other.text
    override fun checkType() = Prim.string
    override fun checkReferenceType() = Prim.string
    override fun checkUsageType() = Prim.string
}