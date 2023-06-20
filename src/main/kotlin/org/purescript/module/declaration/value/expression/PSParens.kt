package org.purescript.module.declaration.value.expression

import com.intellij.lang.ASTNode
import org.purescript.inference.Scope
import org.purescript.inference.Type
import org.purescript.psi.PSPsiElement
import org.purescript.typechecker.TypeCheckerType

class PSParens(node: ASTNode) : PSPsiElement(node), ExpressionAtom {
    val value get() = findChildByClass(Expression::class.java)
    override fun checkType(): TypeCheckerType? = value?.checkType()
    override fun infer(scope: Scope): Type = value!!.infer(scope)
}