package org.purescript.psi.binder

import com.intellij.lang.ASTNode
import com.intellij.psi.util.childrenOfType
import org.purescript.psi.base.PSPsiElement

class Parameters(node: ASTNode) : PSPsiElement(node) {
    val binders get() = binderAtoms.flatMap { it.binders }
    val binderAtoms = childrenOfType<Parameter>().flatMap { it.binderAtoms }
}