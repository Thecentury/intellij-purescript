package org.purescript.parser

class ParserDefinitions {

    // Literals
    private val boolean = `true` / `false`
    private val number = NumericLiteral(NATURAL / FLOAT)
    private val moduleName = ModuleName(!MODULE_PREFIX + PROPER_NAME)
    private val qualifier = ModuleName(MODULE_PREFIX)
    private fun qualified(p: DSL) = !(qualifier) + p

    // ElementTokens

    private val idents =
        Identifier(IDENT / `as` / HIDING / `'forall'` / QUALIFIED / KIND / `'type'`)

    private val lname = Identifier(
        IDENT / data / `'newtype'` / `'type'` / `'foreign'` / `'import'` /
            infixl / infixr / infix / `class` / `'derive'` / KIND /
            `'instance'` / `'module'` / case / of / `if` / then / `else` / `do` /
            ado / let / `true` / `false` / `in` / where / `'forall'` / QUALIFIED /
            HIDING / `as`
    )

    private val label = string / lname

    // this doesn't match parser.y but i dont feel like changing it right now
    // it might be due to differences in the lexer
    private val operator =
        OPERATOR / dot / ddot / ldarrow / OPTIMISTIC / "<=" / "-" / "#" / ":"

    private val properName: DSL = ProperName(PROPER_NAME)
    private val qualProperName = QualifiedProperName(qualified(properName))
    private val type: DSL = Type(Reference { type1 }.sepBy1(dcolon))

    private val forAll = ForAll(
        `'forall'` + +idents + dot + Reference { constrainedType }
    )

    private val rowLabel = Identifier(label) + dcolon + type
    private val row =
        Row((`|` + type) / (rowLabel.sepBy(`,`) + !(`|` + type)))

    private val typeCtor = TypeConstructor(qualProperName)
    private val typeAtom: DSL = TypeAtom(
        squares(!type) /
            ObjectType(braces(row)) /
            `_` /
            string /
            number /
            typeCtor /
            forAll.heal /
            idents /
            parens(arrow / row).heal /
            parens(type)
    )

    private fun braces(p: DSL) = LCURLY + p + RCURLY
    private fun parens(p: DSL) = LPAREN + p + RPAREN
    private fun squares(p: DSL) = LBRACK + p + RBRACK

    private val constrainedType = ConstrainedType(
        !(parens((typeCtor + !+typeAtom).sepBy1(`,`)) + darrow).heal + type
    )

    private val ident = idents / parens(Identifier(operator)).heal
    private val typeVar = TypeVarName(idents) /
        TypeVarKinded(parens(idents + dcolon + type))
    private val binderAtom: DSL = Reference {
        Choice.of(
            NullBinder(`_`),
            CharBinder(char),
            StringBinder(string),
            NumberBinder(number),
            ObjectBinder(squares(binder.sepBy(`,`))),
            braces(recordBinder.sepBy(`,`)),
            parens(binder),
            BooleanBinder(boolean),
            ConstructorBinder(qualProperName),
            NamedBinder(VarBinder(ident) + `@` + this).heal,
            VarBinder(ident),
        )
    }
    private val binder: DSL = Reference { binder1 } + !(dcolon + type)
    private val expr = Value(Reference { expr1 } + !(dcolon + type))
    private val operatorName = OperatorName(operator)
    private val qualOp = QualifiedOperatorName(qualified(operatorName))
    private val type5 = +typeAtom
    private val type4 = ("-".dsl + number) / (!+"#" + type5)
    private val type3 = type4.sepBy1(qualOp)
    private val type2: DSL = type3 + !(arrow / darrow + Reference { type1 })
    private val type1 = !+(`'forall'` + +typeVar + dot) + type2
    private val propertyUpdate: DSL = label + !eq + expr
    private val hole = TypeHole("?".dsl + idents)
    val symbol = Symbol(parens(operatorName))
    private val recordLabel = ObjectBinderField(
        ((label + ":").heal + expr) /
            ((label + eq).heal + expr) /
            ExpressionIdentifier(QualifiedIdentifier(label))
    )
    private val exprAtom = Choice.of(
        `_`,
        hole.heal,
        ExpressionIdentifier(QualifiedIdentifier(qualified(idents))).heal,
        ExpressionSymbol(QualifiedSymbol(qualified(symbol))).heal,
        ExpressionConstructor(qualProperName).heal,
        BooleanLiteral(boolean),
        CharLiteral(char),
        StringLiteral(string),
        number,
        ArrayLiteral(squares(expr.sepBy(`,`))),
        ObjectLiteral(braces(recordLabel.sepBy(`,`))),
        Parens(parens(expr)),
    )
    private val expr7 = exprAtom + !+Accessor(dot + label)


    private val badSingleCaseBranch: DSL =
        Reference { `L{` + binder1 + (arrow + `L}` + exprWhere) / (`L}` + guardedCase) }

    /*
    * if there is only one case branch it can ignore layout so we need
    * to allow layout end at any time.
    */
    private val exprCase: DSL = Case(
        case + expr.sepBy1(`,`) + of + Choice.of(
            badSingleCaseBranch.heal,
            `L{` + Reference { caseBranch }.sepBy1(`L-sep`) + `L}`
        )
    )
    private val expr5 = Reference {
        braces(propertyUpdate.sepBy1(`,`)).heal /
            expr7 /
            Lambda(backslash + +binderAtom + arrow + expr) /
            exprCase /
            ifThenElse /
            doBlock /
            (adoBlock + `in` + expr) /
            letIn
    }
    private val expr4: DSL = +expr5
    private val expr3 = UnaryMinus(+"-".dsl + expr4) / expr4
    private val exprBacktick2 = expr3.sepBy1(qualOp)
    private val expr2 = expr3.sepBy1(tick + exprBacktick2 + tick)
    private val expr1 = expr2.sepBy1(ExpressionOperator(qualOp.heal))

    // TODO: pattern guards should parse expr1 not expr
    private val patternGuard = !(binder + larrow).heal + expr
    private val guard = Guard(`|` + patternGuard.sepBy(`,`))
    private val dataHead = data + properName + TypeArgs(!+typeVar)
    private val dataCtor = DataConstructor(properName + !+typeAtom)
    private val typeDeclaration = Signature(ident + dcolon + type)
    private val newtypeHead = `'newtype'` + properName + TypeArgs(!+typeVar)
    private val exprWhere: DSL = expr + !ExpressionWhere(
        where + `L{` + Reference { letBinding }.sepBy1(`L-sep`) + `L}`
    )
    private val guardedDeclExpr = guard + eq + exprWhere
    private val guardedDecl = (eq.heal + exprWhere) / +guardedDeclExpr
    private val instBinder =
        Choice.of(
            (ident + dcolon).heal + type,
            ValueDeclaration(ident + !+binderAtom + guardedDecl)
        )
    private val foreignDeclaration = `'foreign'` + `'import'` + Choice.of(
        ForeignDataDeclaration(data + properName + dcolon + type),
        ForeignValueDeclaration(ident.heal + dcolon + type)
    )
    private val fixity = Fixity(infixl / infixr / infix + NATURAL)
    private val qualIdentifier = QualifiedIdentifier(!qualifier + ident)
    private val fixityDeclaration = FixityDeclarationType(
        fixity + Choice.of(
            // TODO Should we differentiate Types and DataConstructors?
            // that would mean that when there is a `type` prefix we parse as Type
            // otherwise if it's a capital name it's a DataConstructor
            (!`'type'` + properName / qualProperName).heal,
            qualIdentifier
        ) + `as` + operatorName
    )

    private val fundep = ClassFunctionalDependency(type)
    private val fundeps = `|` + fundep.sepBy1(`,`)
    private val constraint =
        ClassConstraint(ClassName(qualProperName) + !+typeAtom)
    private val constraints = parens(constraint.sepBy1(`,`)) / constraint
    private val classSuper =
        ClassConstraintList(constraints + pImplies(ldarrow))
    private val classNameAndFundeps =
        ClassName(properName) + !+typeVar +
            !ClassFunctionalDependencyList(fundeps)
    private val classSignature = ClassName(properName) + dcolon + type

    // this first is described in haskell code and not in normal happy expression
    // see `fmap (Left . DeclKindSignature () $1) parseClassSignature`
    private val classHead = 
        `class` + classSignature.heal / (!classSuper.heal + classNameAndFundeps)
    private val classMember = ClassMember(idents + dcolon + type)
    private val classDeclaration = ClassDeclaration(
        classHead + !ClassMemberList(
            where + `L{` + classMember.sepBy1(`L-sep`) + `L}`
        ).heal
    )
    private val instHead =
        `'instance'` + !(ident + dcolon) + !(constraints + darrow)
            .heal + constraint // this constraint is the instance type
    private val importedDataMembers = ImportedDataMemberList(
        parens(ddot / ImportedDataMember(properName).sepBy(`,`))
    )
    private val importedItem =
        Choice.of(
            ImportedType(`'type'` + parens(Identifier(operator))),
            ImportedClass(`class` + properName),
            ImportedKind(KIND + properName),
            ImportedOperator(symbol),
            ImportedValue(ident),
            ImportedData(properName + !importedDataMembers),
        )
    private val importList =
        ImportList(!HIDING + parens(importedItem.sepBy(`,`)))
    private val importDeclaration = ImportType(
        `'import'` + moduleName + !importList + !ImportAlias(`as` + moduleName)
    )

    /**
     * nominal = the type can never be coerced to another type.
     * representational = the type can be coerced to another type if certain conditions apply.
     * phantom - the type can always be coerced to another type.
     * */
    private val role = Choice.of(nominal, representational, phantom)
    private val decl = Choice.of(
        (dataHead + dcolon).heal + type,
        DataDeclaration(
            dataHead + !DataConstructorList(eq + dataCtor.sepBy1(`|`))
        ),
        (`'newtype'` + properName + dcolon).heal + type,
        NewtypeDeclaration(
            newtypeHead + eq + NewTypeConstructor(properName + typeAtom)
        ),
        typeDeclaration.heal,
        (`'type'` + `'role'`).heal + properName + !+role,
        (`'type'` + properName + dcolon).heal + type,
        TypeSynonymDeclaration(`'type'` + properName + !+typeVar + eq + type),
        ValueDeclaration(ident.heal + !+binderAtom + guardedDecl),
        foreignDeclaration,
        fixityDeclaration,
        classDeclaration,
        InstanceDeclaration(
            !(`'derive'` + !`'newtype'`) + instHead
                + !(where + `L{` + instBinder.sepBy1(`L-sep`) + `L}`)
        )
    )
    private val exportedClass = ExportedClassType(`class` + properName)
    private val dataMembers = ExportedDataMemberList(
        parens(ddot / ExportedDataMember(properName).sepBy(`,`))
    )
    private val exportedData = ExportedDataType(properName + !dataMembers)
    private val exportedKind = ExportedKindType(KIND + properName)
    private val exportedModule = ExportedModuleType(`'module'` + moduleName)
    private val exportedOperator = ExportedOperatorType(symbol)
    private val exportedType =
        ExportedTypeType(`'type'` + parens(Identifier(operator)))
    private val exportedValue = ExportedValueType(ident)
    private val exportList = ExportListType(
        parens(
            Choice.of(
                exportedClass,
                exportedData,
                exportedKind,
                exportedModule,
                exportedOperator,
                exportedType,
                exportedValue,
            ).sepBy1(`,`)
        )
    )

    private val elseDecl = `else` + !`L-sep`

    val moduleHeader =
        `'module'` + moduleName + !exportList + where + `L{` +
            !+(importDeclaration + `L-sep`)
    val moduleBody = !+(decl.sepBy(elseDecl) + `L-sep`) + `L}`
    val module = ModuleType(moduleHeader + moduleBody)
    private val binder2 = Choice.of(
        (ConstructorBinder(qualProperName) + !+binderAtom).heal,
        NumberBinder(("-".dsl + number).heal),
        binderAtom,
    )
    private val binder1 = binder2.sepBy1(qualOp)
    private val guardedCaseExpr = guard + arrow + exprWhere
    private val guardedCase = (arrow + exprWhere).heal / !+guardedCaseExpr
    private val caseBranch = CaseAlternative(binder1.sepBy1(`,`) + guardedCase)

    private val ifThenElse =
        IfThenElse(`if` + expr + then + expr + `else` + expr)
    private val letBinding =
        Choice.of(
            typeDeclaration.heal,
            ValueDeclaration(ident + !+binderAtom + guardedDecl).heal,
            (binder1 + eq + exprWhere).heal,
            (ident + !+binderAtom + guardedDecl).heal
        )
    private val letIn =
        Let(let + `L{` + (letBinding).sepBy1(`L-sep`) + `L}` + `in` + expr)
    private val doStatement = Choice.of(
        DoNotationLet(let + `L{` + letBinding.sepBy1(`L-sep`) + `L}`),
        DoNotationBind(binder + larrow + expr).heal,
        DoNotationValue(expr).heal
    )
    private val doBlock = DoBlock(
        qualified(`do`).heal + `L{` + (doStatement).sepBy1(`L-sep`) + `L}`
    )

    private val adoBlock = ado + `L{` + doStatement.sepBy(`L-sep`) + `L}`
    private val recordBinder =
        ((label + eq / ":").heal + binder) / VarBinder(label)
}
