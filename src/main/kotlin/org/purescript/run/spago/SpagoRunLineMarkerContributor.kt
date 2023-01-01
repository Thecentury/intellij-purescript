package org.purescript.run.spago

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.purescript.psi.declaration.PSValueDeclaration

class SpagoRunLineMarkerContributor: RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        if (element.firstChild != null) return null
        val decl = element.parentOfType<PSValueDeclaration>() ?: return null
        return if (decl.nameIdentifier.firstChild == element && decl.name == "main") {
            val actions = ExecutorAction.getActions(0)
            Info(
                AllIcons.RunConfigurations.TestState.Run,
                null,
                *actions
            )
        } else {
            null
        }
    }
}