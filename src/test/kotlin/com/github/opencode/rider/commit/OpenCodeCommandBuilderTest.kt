package com.github.opencode.rider.commit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenCodeCommandBuilderTest {
    @Test
    fun `command mode uses custom command and model without prompt text`() {
        val args = OpenCodeCommandBuilder.build(
            state = OpenCodeCommitSettings.State(
                opencodePath = "C:/tools/opencode.cmd",
                model = "openai/gpt-5.5",
                customCommandName = "commit-message",
                promptTemplate = "這段 prompt 不應在 command mode 出現",
            ),
            projectPath = "C:/repo/project",
            diffFilePath = "C:/temp/selected-diff.patch",
        )

        assertEquals("C:/tools/opencode.cmd", args[0])
        assertContainsInOrder(args, "run", "--format", "json", "--model", "openai/gpt-5.5", "--command", "commit-message", "-f", "C:/temp/selected-diff.patch", "--dir", "C:/repo/project")
        assertFalse(args.any { it.contains("這段 prompt") })
    }

    @Test
    fun `prompt mode omits command and appends prompt template`() {
        val args = OpenCodeCommandBuilder.build(
            state = OpenCodeCommitSettings.State(
                promptTemplate = "請根據附件 diff 產生 commit message",
            ),
            projectPath = "C:/repo/project",
            diffFilePath = "C:/temp/selected-diff.patch",
        )

        assertFalse(args.contains("--command"))
        assertFalse(args.contains("--model"))
        assertTrue(args.last().contains("請根據附件 diff 產生 commit message"))
        assertContainsInOrder(args, "opencode", "run", "--format", "json", "-f", "C:/temp/selected-diff.patch", "--dir", "C:/repo/project")
    }

    @Test
    fun `prompt mode forbids additional git commands by default`() {
        val args = OpenCodeCommandBuilder.build(
            state = OpenCodeCommitSettings.State(
                promptTemplate = "請根據附件 diff 產生 commit message",
                allowAdditionalGitCommands = false,
            ),
            projectPath = "C:/repo/project",
            diffFilePath = "C:/temp/selected-diff.patch",
        )

        val prompt = args.last()
        assertTrue(prompt.contains("只使用附件 diff"))
        assertTrue(prompt.contains("不要執行 git status"))
        assertTrue(prompt.contains("git diff"))
        assertTrue(prompt.contains("git log"))
    }

    @Test
    fun `prompt mode allows additional git commands when enabled`() {
        val args = OpenCodeCommandBuilder.build(
            state = OpenCodeCommitSettings.State(
                promptTemplate = "請根據附件 diff 產生 commit message",
                allowAdditionalGitCommands = true,
            ),
            projectPath = "C:/repo/project",
            diffFilePath = "C:/temp/selected-diff.patch",
        )

        val prompt = args.last()
        assertTrue(prompt.contains("必要時可以補充查詢 repository context"))
    }

    private fun assertContainsInOrder(values: List<String>, vararg expected: String) {
        var currentIndex = -1
        for (value in expected) {
            val nextIndex = values.indices.firstOrNull { index -> index > currentIndex && values[index] == value } ?: -1
            assertTrue(nextIndex > currentIndex, "Expected '$value' after index $currentIndex in $values")
            currentIndex = nextIndex
        }
    }
}
