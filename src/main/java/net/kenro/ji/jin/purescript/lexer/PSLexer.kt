package net.kenro.ji.jin.purescript.lexer

import com.intellij.lexer.*
import com.intellij.psi.tree.TokenSet
import net.kenro.ji.jin.purescript.psi.PSTokens

class PSLexer : LookAheadLexer(
    MergingLexerAdapter(
        MergingLexerAdapter(
            FlexAdapter(_PSLexer(null)),
            TokenSet.create(PSTokens.STRING, PSTokens.STRING_GAP, PSTokens.STRING_ESCAPED)
        ),
    TokenSet.create(PSTokens.MLCOMMENT, PSTokens.SLCOMMENT, PSTokens.WS)
    ))