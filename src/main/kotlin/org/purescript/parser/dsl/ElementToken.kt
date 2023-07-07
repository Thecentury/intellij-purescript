package org.purescript.parser.dsl

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.purescript.parser.DSL

data class ElementToken(val token: IElementType) : DSL {
    override val tokenSet: TokenSet = TokenSet.create(token)
    override val heal get() = this
    override fun parse(b: PsiBuilder): Boolean =
        if (b.tokenType === token) {
            b.advanceLexer()
            true
        } else {
            false
        }
}