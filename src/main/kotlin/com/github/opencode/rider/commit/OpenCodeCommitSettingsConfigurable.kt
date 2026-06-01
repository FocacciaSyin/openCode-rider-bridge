package com.github.opencode.rider.commit

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class OpenCodeCommitSettingsConfigurable(
    private val settings: OpenCodeCommitSettings = ApplicationManager.getApplication().getService(OpenCodeCommitSettings::class.java),
) : Configurable {
    private var panel: OpenCodeCommitSettingsPanel? = null

    override fun getDisplayName(): String = "OpenCode Commit Message"

    override fun createComponent(): JComponent {
        val createdPanel = OpenCodeCommitSettingsPanel()
        createdPanel.loadFrom(settings.state)
        panel = createdPanel
        return createdPanel.component
    }

    override fun isModified(): Boolean {
        return panel?.isModifiedFrom(settings.state) ?: false
    }

    override fun apply() {
        val currentPanel = panel ?: return
        val errors = currentPanel.validateInput()
        if (errors.isNotEmpty()) {
            throw ConfigurationException(errors.joinToString("\n"))
        }
        settings.loadState(currentPanel.toState())
    }

    override fun reset() {
        panel?.loadFrom(settings.state)
    }

    override fun disposeUIResources() {
        panel = null
    }
}
