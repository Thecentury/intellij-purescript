package org.purescript.psi.module

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.*
import com.intellij.util.containers.addIfNotNull
import org.purescript.features.DocCommentOwner
import org.purescript.icons.PSIcons
import org.purescript.ide.formatting.ImportDeclaration
import org.purescript.parser.FixityDeclType
import org.purescript.parser.WHERE
import org.purescript.psi.PSElementType
import org.purescript.psi.PSPsiFactory
import org.purescript.psi.base.AStub
import org.purescript.psi.base.PSStubbedElement
import org.purescript.psi.declaration.Importable
import org.purescript.psi.declaration.classes.ClassDecl
import org.purescript.psi.declaration.classes.PSClassMember
import org.purescript.psi.declaration.data.DataConstructor
import org.purescript.psi.declaration.data.DataDeclaration
import org.purescript.psi.declaration.fixity.FixityDeclaration
import org.purescript.psi.declaration.foreign.ForeignValueDecl
import org.purescript.psi.declaration.foreign.PSForeignDataDeclaration
import org.purescript.psi.declaration.imports.Import
import org.purescript.psi.declaration.newtype.NewtypeCtor
import org.purescript.psi.declaration.newtype.NewtypeDecl
import org.purescript.psi.declaration.type.TypeDecl
import org.purescript.psi.declaration.value.ValueDecl
import org.purescript.psi.declaration.value.ValueDeclarationGroup
import org.purescript.psi.exports.*
import org.purescript.psi.name.PSModuleName
import org.purescript.psi.type.PSType

class Module : PsiNameIdentifierOwner, DocCommentOwner,
    PSStubbedElement<Module.Stub>, Importable {
    object Type : PSElementType.WithPsiAndStub<Stub, Module>("Module") {
        override fun createPsi(node: ASTNode) = Module(node)
        override fun createPsi(stub: Stub) = Module(stub, this)
        override fun createStub(psi: Module, p: StubElement<*>) =
            Stub(psi.name, p)

        override fun serialize(stub: Stub, data: StubOutputStream) {
            data.writeName(stub.name)
        }

        override fun deserialize(d: StubInputStream, p: StubElement<*>?) =
            Stub(d.readNameString()!!, p)

        override fun indexStub(stub: Stub, sink: IndexSink) {
            sink.occurrence(ModuleNameIndex.KEY, stub.name)
        }

    }

    class Stub(val name: String, p: StubElement<*>?) : AStub<Module>(p, Type) {
        val exportList: ExportList.Stub?
            get() = childrenStubs
                .filterIsInstance<ExportList.Stub>()
                .firstOrNull()
    }


    constructor(stub: Stub, t: IStubElementType<*, *>) : super(stub, t)
    constructor(node: ASTNode) : super(node)

    override fun asImport() = ImportDeclaration(name)
    override val type: PSType? get() = null

    // TODO clean up this name
    override fun toString(): String = "PSModule($elementType)"
    var cache: Cache = Cache()

    val exports get() = child<ExportList>()
    val fixityDeclarations get() = children(FixityDeclType)

    inner class Cache {
        val exportedItems by lazy { exports?.exportedItems }
        val classDeclarations by lazy { children<ClassDecl>() }
        val imports by lazy { children<Import>() }
        val importsByName by lazy { imports.groupBy { it.name } }
        val importsByAlias by lazy { imports.groupBy { it.importAlias?.name } }
        val importsByModule by lazy { imports.groupBy { it.moduleName.name } }
        val valueDeclarations: Array<ValueDecl> by lazy {
            valueDeclarationGroups
                .flatMap { it.valueDeclarations.asSequence() }
                .toTypedArray()
        }
        val valueDeclarationGroups by lazy { children<ValueDeclarationGroup>() }
        val dataDeclarations by lazy { children<DataDeclaration.Psi>() }
        val dataConstructors by lazy { dataDeclarations.flatMap { it.dataConstructors.toList() } }
        val newTypeDeclarations by lazy { children<NewtypeDecl>() }
        val newTypeConstructors by lazy { newTypeDeclarations.map { it.newTypeConstructor } }
        val typeSynonymDeclarations by lazy { children<TypeDecl>() }
        val classes by lazy { children<ClassDecl>() }
        val foreignValueDeclarations by lazy { children<ForeignValueDecl>() }
        val foreignDataDeclarations by lazy { children<PSForeignDataDeclaration>() }
    }

    override fun subtreeChanged() {
        cache = Cache()
        super.subtreeChanged()
    }

    override fun getName(): String = greenStub?.name ?: nameIdentifier.name

    override fun setName(name: String): PsiElement? {
        val properName =
            project.service<PSPsiFactory>().createModuleName(name)
                ?: return null
        nameIdentifier.replace(properName)
        return this
    }

    override fun getNameIdentifier(): PSModuleName {
        return findNotNullChildByClass(PSModuleName::class.java)
    }

    override fun getTextOffset(): Int = nameIdentifier.textOffset

    /**
     * @return the [FixityDeclaration] that this module exports,
     * both directly and through re-exported modules
     */
    val exportedFixityDeclarations: Sequence<FixityDeclaration>
        get() {
            val explicitlyExportedItems = cache.exportedItems
            return if (explicitlyExportedItems == null) {
                fixityDeclarations.asSequence()
            } else sequence {
                val explicitlyNames = explicitlyExportedItems
                    .filterIsInstance(ExportedOperator.Psi::class.java)
                    .map { it.name }
                    .toSet()

                val exportsSelf = explicitlyExportedItems
                    .filterIsInstance<ExportedModule>()
                    .any { it.name == name }

                if (exportsSelf) {
                    yieldAll(fixityDeclarations.asSequence())
                } else {
                    yieldAll(fixityDeclarations.filter { it.name in explicitlyNames })
                }

                yieldAll(
                    explicitlyExportedItems
                        .asSequence()
                        .filterIsInstance<ExportedModule>()
                        .filter { it.name != name }
                        .flatMap { it.importDeclarations }
                        .flatMap { it.importedFixityDeclarations }
                )
            }
        }
    fun exportedFixityDeclarations(name: String): Sequence<FixityDeclaration> {
        val explicitlyExportedItems = cache.exportedItems
        return if (explicitlyExportedItems == null) {
            fixityDeclarations.asSequence().filter { it.name == name }
        } else sequence {
            val explicitlyNames = explicitlyExportedItems
                .filterIsInstance(ExportedOperator.Psi::class.java)
                .map { it.name }
                .toSet()

            val exportsSelf = explicitlyExportedItems.filterIsInstance<ExportedModule>().any { it.name == name }

            if (exportsSelf || name in explicitlyNames) {
                yieldAll(fixityDeclarations.filter { it.name == name })
            }
            
            yieldAll(
                explicitlyExportedItems
                    .asSequence()
                    .filterIsInstance<ExportedModule>()
                    .filter { it.name != name }
                    .flatMap { it.importDeclarations }
                    .flatMap { it.importedFixityDeclarations(name) }
            )
        }
    }
    /**
     * @return the where keyword in the module header
     */
    val whereKeyword: PsiElement
        get() = findNotNullChildByType(WHERE)

    /**
     * Helper method for retrieving various types of exported declarations.
     *
     * @param declarations The declarations of the wanted type in this module
     * @return the [Declaration] element that this module exports
     */
    private inline fun <Declaration : PsiNamedElement, reified Wanted : ExportedItem<*>> getExportedDeclarations(
        declarations: Array<Declaration>,
        getDeclarations: (Import) -> List<Declaration>
    ): List<Declaration> {
        val explicitlyExportedItems = cache.exportedItems
        return if (explicitlyExportedItems == null) {
            declarations.toList()
        } else {
            val explicitlyNames = explicitlyExportedItems
                .filterIsInstance(Wanted::class.java)
                .map { it.name }
                .toSet()

            val exportsSelf = explicitlyExportedItems
                .filterIsInstance<ExportedModule>()
                .any { it.name == name }

            val exportedDeclarations = mutableListOf<Declaration>()
            if (exportsSelf) {
                exportedDeclarations.addAll(declarations)
            } else {
                declarations.filterTo(exportedDeclarations) { it.name in explicitlyNames }
            }

            explicitlyExportedItems.filterIsInstance<ExportedModule>()
                .filter { it.name != name }
                .flatMap { it.importDeclarations }
                .flatMapTo(exportedDeclarations) {
                    getDeclarations(it)
                }
            exportedDeclarations
        }
    }

    /**
     * @return the [ValueDeclarationGroup] that this module exports,
     * both directly and through re-exported modules
     */
    val exportedValueDeclarationGroups: List<ValueDeclarationGroup>
        get() {
            val explicitlyExportedItems = cache.exportedItems
            return if (explicitlyExportedItems == null) {
                cache.valueDeclarationGroups.toList()
            } else {
                val explicitlyNames = explicitlyExportedItems
                    .filterIsInstance(ExportedValue.Psi::class.java)
                    .map { it.name }
                    .toSet()
                val exportedModules = explicitlyExportedItems.filterIsInstance<ExportedModule>().toList()

                val exportsSelf = exportedModules.any { it.name == name }
                val local = if (exportsSelf) {
                    cache.valueDeclarationGroups.toList()
                } else {
                    cache.valueDeclarationGroups.filter { it.name in explicitlyNames }
                }
                val fromImports = exportedModules
                    .filter { it.name != name }
                    .flatMap { it.importDeclarations }
                    .flatMap { it.importedValueDeclarationGroups }
                (local + fromImports).toList()
            }
        }

    /**
     * @return the [ForeignValueDecl] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedForeignValueDeclarations: List<ForeignValueDecl>
        get() = getExportedDeclarations<ForeignValueDecl, ExportedValue.Psi>(
            cache.foreignValueDeclarations,
        ) { it.importedForeignValueDeclarations }

    /**
     * @return the [PSForeignDataDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedForeignDataDeclarations: List<PSForeignDataDeclaration>
        get() = getExportedDeclarations<PSForeignDataDeclaration, ExportedData.Psi>(
            cache.foreignDataDeclarations,
        ) { it.importedForeignDataDeclarations }

    /**
     * @return the [NewtypeDecl] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedNewTypeDeclarations: List<NewtypeDecl>
        get() = getExportedDeclarations<NewtypeDecl, ExportedData.Psi>(
            cache.newTypeDeclarations,
        ) { it.importedNewTypeDeclarations }

    /**
     * @return the [NewtypeCtor] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedNewTypeConstructors: List<NewtypeCtor>
        get() {
            val explicitlyExportedItems = exports?.exportedItems
                ?: return cache.newTypeConstructors

            val exportedNewTypeConstructors =
                mutableListOf<NewtypeCtor>()

            for (exportedData in explicitlyExportedItems.filterIsInstance<ExportedData.Psi>()) {
                if (exportedData.exportsAll) {
                    exportedNewTypeConstructors.addIfNotNull(exportedData.newTypeDeclaration?.newTypeConstructor)
                } else {
                    exportedData.dataMembers
                        .mapNotNull { it.reference.resolve() }
                        .filterIsInstanceTo(exportedNewTypeConstructors)
                }
            }

            explicitlyExportedItems.filterIsInstance<ExportedModule>()
                .flatMap { it.importDeclarations }
                .flatMapTo(exportedNewTypeConstructors) { it.importedNewTypeConstructors }

            return exportedNewTypeConstructors
        }

    /**
     * @return the [DataDeclaration.Psi] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedDataDeclarations: List<DataDeclaration.Psi>
        get() = getExportedDeclarations<DataDeclaration.Psi, ExportedData.Psi>(
            cache.dataDeclarations,
        ) { it.importedDataDeclarations }

    /**
     * @return the [DataConstructor] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedDataConstructors: List<DataConstructor>
        get() {
            val explicitlyExportedItems = exports?.exportedItems
                ?: return cache.dataConstructors

            val exportedDataConstructors =
                mutableListOf<DataConstructor>()

            for (exportedData in explicitlyExportedItems.filterIsInstance<ExportedData.Psi>()) {
                if (exportedData.exportsAll) {
                    exportedData.dataDeclaration?.dataConstructors
                        ?.mapTo(exportedDataConstructors) { it }
                } else {
                    exportedData.dataMembers
                        .mapNotNull { it.reference.resolve() }
                        .filterIsInstanceTo(exportedDataConstructors)
                }
            }

            explicitlyExportedItems.filterIsInstance<ExportedModule>()
                .flatMap { it.importDeclarations }
                .flatMapTo(exportedDataConstructors) { it.importedDataConstructors }

            return exportedDataConstructors
        }

    /**
     * @return the [TypeDecl] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedTypeSynonymDeclarations: List<TypeDecl>
        get() = getExportedDeclarations<TypeDecl, ExportedData.Psi>(
            cache.typeSynonymDeclarations,
        ) { it.importedTypeSynonymDeclarations }

    /**
     * @return the [ClassDecl] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedClassDeclarations: List<ClassDecl>
        get() = getExportedDeclarations<ClassDecl, ExportedClass.Psi>(
            cache.classes,
        ) { it.importedClassDeclarations }

    /**
     * @return the [PSClassMember] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedClassMembers: List<PSClassMember>
        get() = getExportedDeclarations<PSClassMember, ExportedValue.Psi>(
            cache.classes
                .flatMap { it.classMembers.asSequence() }
                .toTypedArray(),
        ) { it.importedClassMembers }

    val reexportedModuleNames: List<String>
        get() =
            exports?.exportedItems
                ?.filterIsInstance(ExportedModule::class.java)
                ?.map { it.name }
                ?.toList()
                ?: emptyList()

    val exportedNames: List<String>
        get() =
            exports?.exportedItems
                ?.filter { it !is ExportedModule }
                ?.map { it.text.trim() }
                ?.toList()
                ?: emptyList()

    override val docComments: List<PsiComment>
        get() = getDocComments()

    fun addImportDeclaration(importDeclaration: ImportDeclaration) {
        val imports = cache.imports.filter {
            it.moduleName.name == importDeclaration.moduleName &&
                    it.importAlias?.name == importDeclaration.alias
        }
        if (imports.any {
                ImportDeclaration.fromPsiElement(it).run {
                    !hiding && (importedItems.isEmpty() || importedItems.containsAll(importDeclaration.importedItems))
                }
            }) return // already imported
        val oldImport = imports.firstOrNull { !it.isHiding }
        if (oldImport != null) {
            val fromPsiElement = ImportDeclaration.fromPsiElement(oldImport)
            val importedItems = fromPsiElement.importedItems + importDeclaration.importedItems
            val mergedImport = fromPsiElement.withItems(*importedItems.toTypedArray())
            val asPsi = project
                .service<PSPsiFactory>()
                .createImportDeclaration(mergedImport)
            oldImport.replace(asPsi)
        } else {
            val asPsi = project
                .service<PSPsiFactory>()
                .createImportDeclaration(importDeclaration)
            addImportDeclaration(asPsi)
        }
    }

    fun addImportDeclaration(importDeclaration: Import) {
        val lastImportDeclaration = cache.imports.lastOrNull()
        val insertPosition = lastImportDeclaration ?: whereKeyword
        val newLine = project.service<PSPsiFactory>().createNewLine()
        addAfter(importDeclaration, insertPosition)
        addAfter(newLine, insertPosition)
        if (lastImportDeclaration == null) {
            addAfter(newLine, insertPosition)
        }
    }

    val exportsSelf: Boolean
        get() =
            exports?.exportedItems
                ?.filterIsInstance<ExportedModule>()
                ?.any { it.name == name }
                ?: true

    override fun getPresentation() = object : ItemPresentation {
        override fun getPresentableText() = name
        override fun getIcon(unused: Boolean) = PSIcons.FILE
        override fun getLocationString() = when (val projectPath = project.guessProjectDir()) {
            null -> containingFile.virtualFile.path
            else -> projectPath.toNioPath().relativize(containingFile.virtualFile.toNioPath()).toString()
        }
    }

    override fun getIcon(flags: Int) = PSIcons.FILE
}
