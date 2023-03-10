package org.purescript.psi.expression.dostmt

import com.intellij.lang.ASTNode
import com.intellij.psi.util.childrenOfType
import org.purescript.psi.base.PSPsiElement
import org.purescript.psi.binder.Binder

class PSDoNotationBind(node: ASTNode) : PSPsiElement(node), DoStatement {
    override val binders get() = childrenOfType<Binder>().flatMap { it.descendantBinders }
}