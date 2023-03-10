package org.purescript.psi.binder

import com.intellij.lang.ASTNode
import org.purescript.psi.expression.ConstructorReference
import org.purescript.psi.expression.Qualified
import org.purescript.psi.name.PSQualifiedProperName

class ConstructorBinder(node: ASTNode) : BinderAtom(node), Qualified {
    /**
     * @return the [PSQualifiedProperName] identifying this constructor
     */
    internal val qualifiedProperName: PSQualifiedProperName
        get() = findNotNullChildByClass(PSQualifiedProperName::class.java)

    override fun getName(): String = qualifiedProperName.name
    override val qualifierName: String? get() = qualifiedProperName.moduleName?.name
    override fun getReference(): ConstructorReference = ConstructorReference(this, this.qualifiedProperName)
}