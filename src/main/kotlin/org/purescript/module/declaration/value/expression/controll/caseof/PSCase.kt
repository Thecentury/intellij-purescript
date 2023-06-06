package org.purescript.module.declaration.value.expression.controll.caseof

import com.intellij.lang.ASTNode
import com.intellij.psi.util.childrenOfType
import org.purescript.module.declaration.value.expression.Expression
import org.purescript.psi.PSPsiElement
import org.purescript.typechecker.TypeCheckerType

class PSCase(node: ASTNode) : PSPsiElement(node), Expression {
    override val expressions: Sequence<Expression>
        get() = super.expressions +
                childrenOfType<CaseAlternative>().asSequence()
                    .flatMap { it.expressions }

    override fun checkType(): TypeCheckerType? {
        return findChildrenByClass(CaseAlternative::class.java)
            .asList()
            .firstNotNullOfOrNull { it.checkType() }
    }
}