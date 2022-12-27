package org.purescript.psi.module

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.*
import com.intellij.psi.stubs.IStubElementType
import com.intellij.util.containers.addIfNotNull
import org.purescript.features.DocCommentOwner
import org.purescript.parser.WHERE
import org.purescript.psi.PSForeignDataDeclaration
import org.purescript.psi.PSForeignValueDeclaration
import org.purescript.psi.PSPsiFactory
import org.purescript.psi.classes.PSClassDeclaration
import org.purescript.psi.classes.PSClassMember
import org.purescript.psi.data.PSDataConstructor
import org.purescript.psi.data.PSDataDeclaration
import org.purescript.psi.declaration.PSFixityDeclaration
import org.purescript.psi.declaration.PSValueDeclaration
import org.purescript.psi.exports.*
import org.purescript.psi.imports.PSImportDeclaration
import org.purescript.psi.name.PSModuleName
import org.purescript.psi.newtype.PSNewTypeConstructor
import org.purescript.psi.newtype.PSNewTypeDeclaration
import org.purescript.psi.typesynonym.PSTypeSynonymDeclaration
import kotlin.reflect.KProperty1


class PSModule :
    PsiNameIdentifierOwner,
    DocCommentOwner,
    StubBasedPsiElement<PSModuleStub>,
    StubBasedPsiElementBase<PSModuleStub> {

    constructor(stub: PSModuleStub, nodeType: IStubElementType<*, *>) :
        super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    var cache: Cache = Cache()

    inner class Cache {
        val name: String by lazy { nameIdentifier.name }
        val exportsList by lazy { findChildByClass(PSExportList::class.java) }

        val importDeclarations: Array<PSImportDeclaration>
            by lazy { findChildrenByClass(PSImportDeclaration::class.java) }

        val importDeclarationByName: Map<String?, List<PSImportDeclaration>>
            by lazy { importDeclarations.groupBy { it.name } }

        val valueDeclarations: Array<PSValueDeclaration>
            by lazy { findChildrenByClass(PSValueDeclaration::class.java) }

        val dataDeclarations: Array<PSDataDeclaration>
            by lazy { findChildrenByClass(PSDataDeclaration::class.java) }
        val dataConstructors
            by lazy { dataDeclarations.flatMap { it.dataConstructors.toList() } }

        val newTypeDeclarations: Array<PSNewTypeDeclaration>
            by lazy { findChildrenByClass(PSNewTypeDeclaration::class.java) }
        val newTypeConstructors: List<PSNewTypeConstructor>
            by lazy { newTypeDeclarations.map { it.newTypeConstructor } }

        val typeSynonymDeclarations: Array<PSTypeSynonymDeclaration>
            by lazy { findChildrenByClass(PSTypeSynonymDeclaration::class.java) }

        val classDeclarations: Array<PSClassDeclaration>
            by lazy { findChildrenByClass(PSClassDeclaration::class.java) }

        val fixityDeclarations: Array<PSFixityDeclaration>
            by lazy { findChildrenByClass(PSFixityDeclaration::class.java) }

        val foreignValueDeclarations: Array<PSForeignValueDeclaration>
            by lazy { findChildrenByClass(PSForeignValueDeclaration::class.java) }
        val foreignDataDeclarations: Array<PSForeignDataDeclaration>
            by lazy { findChildrenByClass(PSForeignDataDeclaration::class.java) }
    }

    override fun subtreeChanged() {
        cache = Cache()
        super.subtreeChanged()
    }

    override fun getName(): String = stub?.name ?: cache.name

    override fun setName(name: String): PsiElement? {
        val properName = PSPsiFactory(project).createModuleName(name)
            ?: return null
        nameIdentifier.replace(properName)
        return this
    }

    override fun getNameIdentifier(): PSModuleName {
        return findNotNullChildByClass(PSModuleName::class.java)
    }

    override fun getTextOffset(): Int = nameIdentifier.textOffset

    fun getImportDeclarationByName(name: String): PSImportDeclaration? {
        return cache.importDeclarations
            .asSequence()
            .find { (it.name ?: "") == name }
    }

    /**
     * @return the [PSFixityDeclaration] that this module exports,
     * both directly and through re-exported modules
     */
    val exportedFixityDeclarations: List<PSFixityDeclaration>
        get() = getExportedDeclarations(
            cache.fixityDeclarations,
            PSImportDeclaration::importedFixityDeclarations,
            PSExportedOperator::class.java
        )

    /**
     * @return the where keyword in the module header
     */
    val whereKeyword: PsiElement
        get() = findNotNullChildByType(WHERE)

    /**
     * Helper method for retrieving various types of exported declarations.
     *
     * @param declarations The declarations of the wanted type in this module
     * @param importedDeclarationProperty The property for the imported declarations in an [PSImportDeclaration]
     * @param exportedItemClass The class of the [PSExportedItem] to use when filtering the results
     * @return the [Declaration] element that this module exports
     */
    private fun <Declaration : PsiNamedElement> getExportedDeclarations(
        declarations: Array<Declaration>,
        importedDeclarationProperty: KProperty1<PSImportDeclaration, List<Declaration>>,
        exportedItemClass: Class<out PSExportedItem>
    ): List<Declaration> {
        val explicitlyExportedItems = cache.exportsList?.exportedItems
            ?: return declarations.toList()

        val explicitlyNames = explicitlyExportedItems
            .filterIsInstance(exportedItemClass)
            .map { it.name }
            .toSet()

        val exportedDeclarations = mutableListOf<Declaration>()

        val exportsSelf = explicitlyExportedItems
            .filterIsInstance<PSExportedModule>()
            .any { it.name == name }

        declarations.filterTo(exportedDeclarations) {
            exportsSelf || it.name in explicitlyNames
        }

        explicitlyExportedItems.filterIsInstance<PSExportedModule>()
            .flatMap { it.importDeclarations }
            .flatMapTo(exportedDeclarations) {
                importedDeclarationProperty.get(
                    it
                )
            }

        return exportedDeclarations
    }

    /**
     * @return the [PSValueDeclaration] that this module exports,
     * both directly and through re-exported modules
     */
    val exportedValueDeclarations: List<PSValueDeclaration>
        get() = getExportedDeclarations(
            cache.valueDeclarations,
            PSImportDeclaration::importedValueDeclarations,
            PSExportedValue::class.java
        )

    /**
     * @return the [PSForeignValueDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedForeignValueDeclarations: List<PSForeignValueDeclaration>
        get() = getExportedDeclarations(
            cache.foreignValueDeclarations,
            PSImportDeclaration::importedForeignValueDeclarations,
            PSExportedValue::class.java
        )

    /**
     * @return the [PSForeignDataDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedForeignDataDeclarations: List<PSForeignDataDeclaration>
        get() = getExportedDeclarations(
            cache.foreignDataDeclarations,
            PSImportDeclaration::importedForeignDataDeclarations,
            PSExportedData::class.java
        )

    /**
     * @return the [PSNewTypeDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedNewTypeDeclarations: List<PSNewTypeDeclaration>
        get() = getExportedDeclarations(
            cache.newTypeDeclarations,
            PSImportDeclaration::importedNewTypeDeclarations,
            PSExportedData::class.java
        )

    /**
     * @return the [PSNewTypeConstructor] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedNewTypeConstructors: List<PSNewTypeConstructor>
        get() {
            val explicitlyExportedItems = cache.exportsList?.exportedItems
                ?: return cache.newTypeConstructors

            val exportedNewTypeConstructors =
                mutableListOf<PSNewTypeConstructor>()

            for (exportedData in explicitlyExportedItems.filterIsInstance<PSExportedData>()) {
                if (exportedData.exportsAll) {
                    exportedNewTypeConstructors.addIfNotNull(exportedData.newTypeDeclaration?.newTypeConstructor)
                } else {
                    exportedData.dataMembers
                        .mapNotNull { it.reference.resolve() }
                        .filterIsInstanceTo(exportedNewTypeConstructors)
                }
            }

            explicitlyExportedItems.filterIsInstance<PSExportedModule>()
                .flatMap { it.importDeclarations }
                .flatMapTo(exportedNewTypeConstructors) { it.importedNewTypeConstructors }

            return exportedNewTypeConstructors
        }

    /**
     * @return the [PSDataDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedDataDeclarations: List<PSDataDeclaration>
        get() = getExportedDeclarations(
            cache.dataDeclarations,
            PSImportDeclaration::importedDataDeclarations,
            PSExportedData::class.java
        )

    /**
     * @return the [PSDataConstructor] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedDataConstructors: List<PSDataConstructor>
        get() {
            val explicitlyExportedItems = cache.exportsList?.exportedItems
                ?: return cache.dataConstructors

            val exportedDataConstructors = mutableListOf<PSDataConstructor>()

            for (exportedData in explicitlyExportedItems.filterIsInstance<PSExportedData>()) {
                if (exportedData.exportsAll) {
                    exportedData.dataDeclaration?.dataConstructors
                        ?.mapTo(exportedDataConstructors) { it }
                } else {
                    exportedData.dataMembers
                        .mapNotNull { it.reference.resolve() }
                        .filterIsInstanceTo(exportedDataConstructors)
                }
            }

            explicitlyExportedItems.filterIsInstance<PSExportedModule>()
                .flatMap { it.importDeclarations }
                .flatMapTo(exportedDataConstructors) { it.importedDataConstructors }

            return exportedDataConstructors
        }

    /**
     * @return the [PSTypeSynonymDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedTypeSynonymDeclarations: List<PSTypeSynonymDeclaration>
        get() = getExportedDeclarations(
            cache.typeSynonymDeclarations,
            PSImportDeclaration::importedTypeSynonymDeclarations,
            PSExportedData::class.java
        )

    /**
     * @return the [PSClassDeclaration] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedClassDeclarations: List<PSClassDeclaration>
        get() = getExportedDeclarations(
            cache.classDeclarations,
            PSImportDeclaration::importedClassDeclarations,
            PSExportedClass::class.java
        )

    /**
     * @return the [PSClassMember] elements that this module exports,
     * both directly and through re-exported modules
     */
    val exportedClassMembers: List<PSClassMember>
        get() = getExportedDeclarations(
            cache.classDeclarations
                .flatMap { it.classMembers.asSequence() }
                .toTypedArray(),
            PSImportDeclaration::importedClassMembers,
            PSExportedValue::class.java
        )

    val reexportedModuleNames: List<String>
        get() =
            cache.exportsList?.exportedItems
                ?.filterIsInstance(PSExportedModule::class.java)
                ?.map { it.name }
                ?.toList()
                ?: emptyList()

    val exportedNames: List<String>
        get() =
            cache.exportsList?.exportedItems
                ?.filter { it !is PSExportedModule }
                ?.map { it.text.trim() }
                ?.toList()
                ?: emptyList()

    override val docComments: List<PsiComment>
        get() = getDocComments()

    fun addImportDeclaration(importDeclaration: PSImportDeclaration) {
        val lastImportDeclaration = cache.importDeclarations.lastOrNull()
        val insertPosition = lastImportDeclaration ?: whereKeyword
        val newLine = PSPsiFactory(project).createNewLine()
        addAfter(importDeclaration, insertPosition)
        addAfter(newLine, insertPosition)
        if (lastImportDeclaration == null) {
            addAfter(newLine, insertPosition)
        }
    }

    val exportsSelf: Boolean
        get() =
            cache.exportsList?.exportedItems
                ?.filterIsInstance<PSExportedModule>()
                ?.any { it.name == name }
                ?: true
    
    override fun toString(): String {
        return "${javaClass.simpleName}($elementType)"
    }
}