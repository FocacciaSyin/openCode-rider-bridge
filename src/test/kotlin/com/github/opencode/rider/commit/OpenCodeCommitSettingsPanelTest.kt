package com.github.opencode.rider.commit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenCodeCommitSettingsPanelTest {
    @Test
    fun `panel round trips all settings fields`() {
        val panel = OpenCodeCommitSettingsPanel()
        val state = OpenCodeCommitSettings.State(
            opencodePath = "C:/tools/opencode.cmd",
            model = "openai/gpt-5.5",
            customCommandName = "commit-message",
            promptTemplate = "請根據附件 diff 產生 commit message",
            allowAdditionalGitCommands = true,
            timeoutSeconds = 120,
            diffSizeLimitBytes = 2_097_152,
            enabledByDefault = true,
        )

        panel.loadFrom(state)

        assertEquals(state, panel.toState())
    }

    @Test
    fun `panel validation accepts normal values`() {
        val panel = OpenCodeCommitSettingsPanel()
        panel.loadFrom(OpenCodeCommitSettings.State())

        assertTrue(panel.validateInput().isEmpty())
    }

    @Test
    fun `panel validation rejects timeout outside safe range`() {
        val panel = OpenCodeCommitSettingsPanel()
        panel.loadFrom(OpenCodeCommitSettings.State(timeoutSeconds = 0))

        val errors = panel.validateInput()

        assertTrue(errors.any { error -> error.contains("timeout", ignoreCase = true) })
    }

    @Test
    fun `panel validation rejects non positive diff size limit`() {
        val panel = OpenCodeCommitSettingsPanel()
        panel.loadFrom(OpenCodeCommitSettings.State(diffSizeLimitBytes = 0))

        val errors = panel.validateInput()

        assertTrue(errors.any { error -> error.contains("diff", ignoreCase = true) })
    }

    @Test
    fun `panel detects modified state`() {
        val panel = OpenCodeCommitSettingsPanel()
        val original = OpenCodeCommitSettings.State()
        panel.loadFrom(original)

        assertFalse(panel.isModifiedFrom(original))

        val changed = original.copy(model = "openai/gpt-5.5")
        panel.loadFrom(changed)

        assertTrue(panel.isModifiedFrom(original))
    }
}
