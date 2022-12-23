package org.purescript.parser

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.purescript.parser.ParserInfo.Failure

class SymbolicParsec(private val ref: Parsec, private val node: IElementType) :
    Parsec() {
    override fun parse(context: ParserContext): ParserInfo {
        val startPosition = context.position
        val pack = context.start()
        val info = ref.parse(context)
        if (info !is Failure) {
            pack.done(node)
        } else {
            pack.drop()
        }
        return if (startPosition == info.position) {
            if (info !is Failure) ParserInfo.Optional(
                info.position,
                setOf(this)
            )
            else ParserInfo.Failure(info.position, setOf(this))
        } else {
            info
        }
    }

    override fun calcExpectedName() = setOf(node.toString())
    override val canStartWithSet: TokenSet get() = ref.canStartWithSet
    public override fun calcCanBeEmpty() = ref.canBeEmpty
}