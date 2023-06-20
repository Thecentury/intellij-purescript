package org.purescript.module.declaration.value.expression.literals

import com.intellij.lang.ASTNode
import org.purescript.inference.Scope
import org.purescript.inference.Type
import org.purescript.module.declaration.value.Similar
import org.purescript.module.declaration.value.expression.ExpressionAtom
import org.purescript.psi.PSPsiElement
import org.purescript.typechecker.Prim

class PSNumericLiteral(node: ASTNode) : PSPsiElement(node), ExpressionAtom {
    override fun areSimilarTo(other: Similar): Boolean =
        other is PSNumericLiteral && other.text == this.text

    override fun checkReferenceType() =
        if (text.contains(".")) Prim.number
        else Prim.int

    override fun infer(scope: Scope) =
        if (text.contains(".")) Type.Number
        else Type.Int
}