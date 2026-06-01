package com.github.opencode.rider.commit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenCodeOutputParserTest {
    @Test
    fun `json event output parses final commit message`() {
        val output = """
            {"type":"message","role":"assistant","text":"chore: ignore intermediate output"}
            {"type":"message","role":"assistant","text":"feat: add before commit opencode generation"}
        """.trimIndent()

        val result = OpenCodeOutputParser.parse(output)

        assertEquals(OpenCodeParseResult.Success("feat: add before commit opencode generation"), result)
    }

    @Test
    fun `plain text output is accepted as fallback`() {
        val result = OpenCodeOutputParser.parse("\nfix: handle empty commit diff\n")

        assertEquals(OpenCodeParseResult.Success("fix: handle empty commit diff"), result)
    }

    @Test
    fun `parser strips ansi codes and markdown code fences`() {
        val result = OpenCodeOutputParser.parse("\u001B[32m```commit\nfeat: clean parser output\n```\u001B[0m")

        assertEquals(OpenCodeParseResult.Success("feat: clean parser output"), result)
    }

    @Test
    fun `empty output fails safely`() {
        val result = OpenCodeOutputParser.parse("   \n  ")

        assertTrue(result is OpenCodeParseResult.Failure)
        assertTrue((result as OpenCodeParseResult.Failure).message.contains("empty", ignoreCase = true))
    }

    @Test
    fun `message longer than limit fails safely`() {
        val result = OpenCodeOutputParser.parse("feat: ${"x".repeat(20)}", maxLength = 10)

        assertTrue(result is OpenCodeParseResult.Failure)
        assertTrue((result as OpenCodeParseResult.Failure).message.contains("too long", ignoreCase = true))
    }
}
