package org.purescript.psi.base

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.purescript.file.ImportedModuleIndex
import org.purescript.file.ModuleNameIndex
import org.purescript.file.PSFile
import org.purescript.psi.module.Module

abstract class PSPsiElement(node: ASTNode, val string: String? = null) :
    ASTWrapperPsiElement(node) {

    override fun toString(): String {
        if (string != null) return string + "(" + node.elementType + ")"
        else return super.toString()
    }

    /**
     * @return the [Module.Psi] containing this element
     */
    val module: Module.Psi? get() = (containingFile as PSFile.Psi).module

    override fun getUseScope(): SearchScope = module
        ?.name
        ?.let {
            val visited = mutableSetOf<VirtualFile>()
            val queue = ArrayDeque<String>()
            queue.addLast(it)
            visited.add(containingFile.virtualFile)
            while (queue.isNotEmpty()) {
                val moduleName = queue.removeFirst()
                val filesImportingModule =
                    ImportedModuleIndex.filesImportingModule(
                        project,
                        moduleName
                    )
                val toQueue = filesImportingModule subtract visited
                visited.addAll(filesImportingModule)
                queue.addAll(toQueue.mapNotNull {
                    ModuleNameIndex.getModuleNameFromFile(
                        project,
                        it
                    )
                })
            }
            GlobalSearchScope.filesScope(project, visited)
        }
        ?: super.getUseScope()
}
