package com.github.opencode.rider.commit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class OpenCodeCommitSettingsTest {
    @Test
    fun `default state uses safe commit message generation settings`() {
        val settings = OpenCodeCommitSettings()
        val state = settings.state

        assertEquals("opencode", state.opencodePath)
        assertEquals("", state.model)
        assertEquals("", state.customCommandName)
        assertEquals(OpenCodeCommitSettings.DEFAULT_PROMPT_TEMPLATE, state.promptTemplate)
        assertFalse(state.allowAdditionalGitCommands)
        assertEquals(60, state.timeoutSeconds)
        assertEquals(1_048_576, state.diffSizeLimitBytes)
        assertFalse(state.enabledByDefault)
    }

    @Test
    fun `loadState replaces all persisted values`() {
        val settings = OpenCodeCommitSettings()
        val loadedState = OpenCodeCommitSettings.State(
            opencodePath = "C:/tools/opencode.cmd",
            model = "openai/gpt-5.5",
            customCommandName = "commit-message",
            promptTemplate = "請依照附件 diff 產生 commit message",
            allowAdditionalGitCommands = true,
            timeoutSeconds = 120,
            diffSizeLimitBytes = 2_097_152,
            enabledByDefault = true,
        )

        settings.loadState(loadedState)

        assertEquals(loadedState, settings.state)
    }
}
