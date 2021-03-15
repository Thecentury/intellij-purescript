package org.purescript.parser

class PSLanguageParserTest : PSLanguageParserTestBase("parser") {
    // modules
    fun testmodule1() = doTest(true, true)
    fun testmodule2() = doTest(true, true)
    fun testmodule_export1() = doTest(true, true)

    // imports
    fun testimport1() = doTest(true, true)
    fun testimport2() = doTest(true, true)

    // declarations
    fun testdeclarations() = doTest(true, true)

    // data declaration
    fun testdata_declaration1() = doTest(true, true)
    fun testdata_declaration2() = doTest(true, true)
    fun testdata_declaration3() = doTest(true, true)
    fun testdata_declaration4() = doTest(true, true)
    fun testdata_declaration5() = doTest(true, true)
    fun testdata_declaration6() = doTest(true, true)
    fun testdata_declaration7() = doTest(true, true)
    fun testdata_declaration8() = doTest(true, true)
    fun testdata_declaration9() = doTest(true, true)
    fun testdata_declaration10() = doTest(true, true)
    fun testdata_declaration11() = doTest(true, true)
    fun testdata_declaration12() = doTest(true, true)

    // type declaration
    fun testtype_declaration1() = doTest(true, true)
    fun testtype_declaration2() = doTest(true, true)
    fun testtype_declaration3() = doTest(true, true)
    fun testtype_declaration4() = doTest(true, true)
    fun testtype_declaration5() = doTest(true, true)
    fun testtype_declaration6() = doTest(true, true)
    fun testtype_declaration7() = doTest(true, true)
    fun testtype_declaration8() = doTest(true, true)
    fun testtype_declaration9() = doTest(true, true)
    fun testtype_declaration10() = doTest(true, true)

    // newtype declaration
    fun testnewtype_declaration3() = doTest(true, true)
    fun testnewtype_declaration4() = doTest(true, true)
    fun testnewtype_declaration5() = doTest(true, true)
    fun testnewtype_declaration7() = doTest(true, true)
    fun testnewtype_declaration8() = doTest(true, true)
    fun testnewtype_declaration9() = doTest(true, true)
    fun testnewtype_declaration10() = doTest(true, true)

    // fixes for purescript examples failure
    fun test1570() = doTest(true, true)
    fun test2049_fixity() = doTest(true, true)
    fun test2288() = doTest(true, true)
    fun test2609() = doTest(true, true)
    fun test2616() = doTest(true, true)
    fun test2695() = doTest(true, true)
    fun test2626() = doTest(true, true)
    fun testAutoPrelude2() = doTest(true, true)
    fun testCaseInputWildcard() = doTest(true, true)
    fun testCaseMultipleExpressions() = doTest(true, true)
    fun testClassRefSyntax() = doTest(true, true)
    fun testDctorName() = doTest(true, true)
    fun testExportedInstanceDeclarations() = doTest(true, true)
    fun testImportHiding() = doTest(true, true)
    fun testPolyLabels() = doTest(true, true)
    fun testQualifiedQualifiedImports() = doTest(true, true)
    fun testDollar() = doTest(true, true)
    fun testConstraintParens() = doTest(true, true)
    fun testConstraintParsingIssue() = doTest(true, true)
    fun testDerivingFunctor() = doTest(true, true)
    fun testFunctionalDependencies() = doTest(true, true)
    fun testGenericsRep() = doTest(true, true)
    fun testIfWildcard() = doTest(true, true)
    fun testMPTCs() = doTest(true, true)
    fun testMonadState() = doTest(true, true)
    fun testNewtypeClass() = doTest(true, true)
    fun testOperatorAlias() = doTest(true, true)
    fun testOperatorAliasElsewhere() = doTest(true, true)
    fun testOperators() = doTest(true, true)
    fun testRebindableSyntax() = doTest(true, true)
    fun testRowInInstanceHeadDetermined() = doTest(true, true)
    fun testRowPolyInstanceContext() = doTest(true, true)
    fun testRowsInInstanceContext() = doTest(true, true)
    fun testSuperclasses3() = doTest(true, true)
    fun testUnicodeType() = doTest(true, true)
    fun testUntupledConstraints() = doTest(true, true)
    fun testUsableTypeClassMethods() = doTest(true, true)
    fun testWildcardInInstance() = doTest(true, true)
    fun testTypeClasses() = doTest(true, true)
    fun testTypedBinders() = doTest(true, true)
    fun testUnicodeOperators() = doTest(true, true)
    fun testDctorOperatorAlias() = doTest(true, true)
    fun testLetPattern() = doTest(true, true)
    fun testTypeOperators() = doTest(true, true)
    fun testTailCall() = doTest(true, true)
    fun testForeignKind() = doTest(true, true)
    fun testStream() = doTest(true, true)
    fun testSolvingAppendSymbol() = doTest(true, true)
    fun testSolvingCompareSymbol() = doTest(true, true)
    fun test2663() = doTest(true, true)
    fun test2378() = doTest(true, true)
    fun test2049_named_pattern_matching() = doTest(true, true)
    fun testDuplicateProperties() = doTest(true, true)
    fun testExtendedInfixOperators() = doTest(true, true)
    fun testFieldPuns() = doTest(true, true)
    fun testFieldConsPuns() = doTest(true, true)
    fun testFunWithFunDeps() = doTest(true, true)
    fun testRowUnion() = doTest(true, true)
    fun testKindedType() = doTest(true, true)
    fun testMutRec2() = doTest(true, true)
    fun testMutRec3() = doTest(true, true)
    fun testNewtypeInstance() = doTest(true, true)
    fun testInt() = doTest(true, true)
    fun testNestedRecordUpdate() = doTest(true, true)
    fun testNestedRecordUpdateWildcards() = doTest(true, true)
    fun testPrimedTypeName() = doTest(true, true)
    fun testRowConstructors() = doTest(true, true)
    fun testGuards() = doTest(true, true)
    fun testDiffKindsSameName() = doTest(true, true)
    fun testProgrammableTypeErrors() = doTest(true, true)
    fun testSuggestComposition() = doTest(true, true)
    fun testTypedHole() = doTest(true, true)
    fun testShadowedNameParens() = doTest(true, true)
    fun testTypeLevelString() = doTest(true, true)

    // Bugs
    fun testbug_do_block1() = doTest(true, true)
    fun testbug_do_block2() = doTest(true, true)
    fun testbug_functions1() = doTest(true, true)
    fun testbug_functions2() = doTest(true, true)
    fun testbug_functions3() = doTest(true, true)
    fun testbug_functions4() = doTest(true, true)
    fun testbug_functions5() = doTest(true, true)
    @Suppress("unused")
    fun xtestbug_instance1() = doTest(true, true)
    fun testbug_newtype1() = doTest(true, true)
    fun testbug_newtype2() = doTest(true, true)
    fun testbug_syntax_sugar1() = doTest(true, true)
    fun testbug_syntax_sugar3() = doTest(true, true)
    fun testbug_syntax_sugar4() = doTest(true, true)
    fun testbug_syntax_sugar2() = doTest(true, true)
    fun testbug_functions6() = doTest(true, true)
    fun testbug_abs1() = doTest(true, true)
    fun testbug_import1() = doTest(true, true)

    // Small regression tests
    fun testNewline() = doTest(true, true)
    fun testAtBinder() = doTest(true, true)
    fun testSnuglyCaseWithWhere() = doTest(true, true)

    // instances
    fun testElseInstance() = doTest(true, true)
}