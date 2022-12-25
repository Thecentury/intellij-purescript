package org.purescript.parser

import com.intellij.psi.tree.TokenSet
import org.purescript.parser.Info.Failure

class SeqParser(
    private val ps: Array<out Parsec>,
    private val first: Parsec
) : Parsec() {
    override fun parse(context: ParserContext): Info {
        var info = first.parse(context)
        for (p in ps) {
            info = when (info) {
                is Failure -> return info
                is Info.Success -> p.parse(context)
                is Info.Optional -> {
                    when (val next = p.parse(context)) {
                        is Info.Optional ->
                            Info.Optional(
                                next.position,
                                info.expected + next.expected
                            )

                        else -> next
                    }
                }
            }
        }
        return info
    }

    override val canStartWithSet: TokenSet
        by lazy {
            var ret = TokenSet.EMPTY
            for (p in all()) {
                ret = TokenSet.orSet(ret, p.canStartWithSet)
                if (!p.canBeEmpty) return@lazy ret
            }
            ret
        }

    private fun all() = sequenceOf(first, *ps)
    override val canBeEmpty: Boolean by lazy { all().all { it.canBeEmpty } }
}