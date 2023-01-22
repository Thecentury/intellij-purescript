package org.purescript.psi.declaration.fixity

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.purescript.psi.declaration.value.ValueDecl

class ExportedFixityNameIndex  : StringStubIndexExtension<FixityDeclaration>() {
    override fun getKey() = KEY

    companion object {
        val KEY = StubIndexKey.createIndexKey<String, FixityDeclaration>(
            "purescript.exported.fixity.decl.name"
        )
    }
}