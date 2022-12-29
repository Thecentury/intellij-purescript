package org.purescript.run

import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project

class SpagoConfigurationFactory: ConfigurationFactory(SpagoConfigurationType) {
    override fun getId() = "SpagoConfigurationFactory"
    override fun createTemplateConfiguration(project: Project) =
        SpagoRunConfiguration(project, this)
}

