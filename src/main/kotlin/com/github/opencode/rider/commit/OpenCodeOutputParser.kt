package com.github.opencode.rider.commit

sealed interface OpenCodeParseResult {
    data class Success(val message: String) : OpenCodeParseResult
    data class Failure(val message: String) : OpenCodeParseResult
}

object OpenCodeOutputParser {
    private val ansiRegex = Regex("\\u001B\\[[;?0-9]*[ -/]*[@-~]")
    private val jsonTextRegex = Regex("\"(?:text|message|content)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")

    fun parse(output: String, maxLength: Int = 5_000): OpenCodeParseResult {
        val withoutAnsi = output.replace(ansiRegex, "").trim()
        if (withoutAnsi.isBlank()) {
            return OpenCodeParseResult.Failure("OpenCode output is empty")
        }

        val jsonMessage = extractJsonMessage(withoutAnsi)
        val candidate = cleanupMessage(jsonMessage ?: withoutAnsi)

        if (candidate.isBlank()) {
            return OpenCodeParseResult.Failure("OpenCode output is empty after cleanup")
        }

        if (candidate.length > maxLength) {
            return OpenCodeParseResult.Failure("OpenCode commit message is too long")
        }

        return OpenCodeParseResult.Success(candidate)
    }

    private fun extractJsonMessage(output: String): String? {
        return output.lineSequence()
            .mapNotNull { line -> jsonTextRegex.find(line)?.groupValues?.get(1) }
            .map(::unescapeJsonString)
            .map(::cleanupMessage)
            .filter { message -> message.isNotBlank() }
            .lastOrNull()
    }

    private fun cleanupMessage(message: String): String {
        var cleaned = message.trim()
        cleaned = cleaned.removePrefix("```commit").trim()
        cleaned = cleaned.removePrefix("```").trim()
        cleaned = cleaned.removeSuffix("```").trim()
        return cleaned
    }

    private fun unescapeJsonString(value: String): String {
        return value
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
