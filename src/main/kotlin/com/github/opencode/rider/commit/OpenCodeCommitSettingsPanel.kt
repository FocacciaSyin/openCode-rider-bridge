package com.github.opencode.rider.commit

import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel

class OpenCodeCommitSettingsPanel {
    private val opencodePathField = JTextField()
    private val modelField = JTextField()
    private val customCommandNameField = JTextField()
    private val promptTemplateArea = JTextArea(5, 40)
    private val timeoutSecondsSpinner = JSpinner(SpinnerNumberModel(60, 1, 600, 1))
    private val diffSizeLimitBytesSpinner = JSpinner(SpinnerNumberModel(1_048_576, 1, Int.MAX_VALUE, 1024))
    private val enabledByDefaultCheckBox = JCheckBox("預設勾選 Before Commit 的 OpenCode commit message 產生")
    private val allowAdditionalGitCommandsCheckBox = JCheckBox("允許 OpenCode 必要時額外執行 git 指令補充上下文")

    val component: JComponent = JPanel(BorderLayout()).apply {
        add(buildForm(), BorderLayout.NORTH)
    }

    fun loadFrom(state: OpenCodeCommitSettings.State) {
        opencodePathField.text = state.opencodePath
        modelField.text = state.model
        customCommandNameField.text = state.customCommandName
        promptTemplateArea.text = state.promptTemplate
        timeoutSecondsSpinner.value = state.timeoutSeconds
        diffSizeLimitBytesSpinner.value = state.diffSizeLimitBytes
        enabledByDefaultCheckBox.isSelected = state.enabledByDefault
        allowAdditionalGitCommandsCheckBox.isSelected = state.allowAdditionalGitCommands
    }

    fun toState(): OpenCodeCommitSettings.State {
        return OpenCodeCommitSettings.State(
            opencodePath = opencodePathField.text.trim(),
            model = modelField.text.trim(),
            customCommandName = customCommandNameField.text.trim(),
            promptTemplate = promptTemplateArea.text.trim(),
            allowAdditionalGitCommands = allowAdditionalGitCommandsCheckBox.isSelected,
            timeoutSeconds = timeoutSecondsSpinner.intValue(),
            diffSizeLimitBytes = diffSizeLimitBytesSpinner.intValue(),
            enabledByDefault = enabledByDefaultCheckBox.isSelected,
        )
    }

    fun isModifiedFrom(state: OpenCodeCommitSettings.State): Boolean {
        return toState() != state
    }

    fun validateInput(): List<String> {
        val errors = mutableListOf<String>()
        if (opencodePathField.text.isBlank()) {
            errors.add("opencode path 不可空白")
        }
        if (timeoutSecondsSpinner.intValue() !in 5..300) {
            errors.add("timeout seconds 必須介於 5 到 300 秒")
        }
        if (diffSizeLimitBytesSpinner.intValue() <= 0) {
            errors.add("diff size limit 必須大於 0")
        }
        val model = modelField.text.trim()
        if (model.isNotEmpty() && !model.contains("/")) {
            errors.add("model 建議使用 provider/model 格式")
        }
        if (promptTemplateArea.text.isBlank()) {
            errors.add("prompt template 不可空白")
        }
        return errors
    }

    private fun buildForm(): JPanel {
        val panel = JPanel(GridBagLayout())
        var row = 0
        panel.addRow(row++, "OpenCode 執行檔路徑", opencodePathField)
        panel.addRow(row++, "Model（可空白）", modelField)
        panel.addRow(row++, "自建 command / skill 名稱（可空白）", customCommandNameField)
        panel.addRow(row++, "Timeout 秒數", timeoutSecondsSpinner)
        panel.addRow(row++, "Diff 大小上限 bytes", diffSizeLimitBytesSpinner)
        panel.addFullRow(row++, enabledByDefaultCheckBox)
        panel.addFullRow(row++, allowAdditionalGitCommandsCheckBox)
        panel.addRow(row++, "Prompt template", JScrollPane(promptTemplateArea))
        panel.addFullRow(
            row,
            JLabel("<html><b>安全提醒：</b>Rider selected changes diff 會送到你設定的 OpenCode model。預設只使用附件 diff，不額外執行 git 指令。</html>"),
        )
        return panel
    }

    private fun JSpinner.intValue(): Int = (value as Number).toInt()

    private fun JPanel.addRow(row: Int, labelText: String, field: JComponent) {
        add(JLabel(labelText), labelConstraints(row))
        add(field, fieldConstraints(row))
    }

    private fun JPanel.addFullRow(row: Int, component: JComponent) {
        add(component, fullRowConstraints(row))
    }

    private fun labelConstraints(row: Int): GridBagConstraints {
        return GridBagConstraints().apply {
            gridx = 0
            gridy = row
            anchor = GridBagConstraints.WEST
            insets.set(4, 4, 4, 8)
        }
    }

    private fun fieldConstraints(row: Int): GridBagConstraints {
        return GridBagConstraints().apply {
            gridx = 1
            gridy = row
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets.set(4, 4, 4, 4)
        }
    }

    private fun fullRowConstraints(row: Int): GridBagConstraints {
        return GridBagConstraints().apply {
            gridx = 0
            gridy = row
            gridwidth = 2
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            insets.set(4, 4, 4, 4)
        }
    }
}
