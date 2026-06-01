package com.github.opencode.rider.commit

object OpenCodeCommandBuilder {
    fun build(
        state: OpenCodeCommitSettings.State,
        projectPath: String,
        diffFilePath: String,
    ): List<String> {
        val args = mutableListOf(
            state.opencodePath,
            "run",
            "--format",
            "json",
        )

        if (state.model.isNotBlank()) {
            args.add("--model")
            args.add(state.model.trim())
        }

        if (state.customCommandName.isNotBlank()) {
            args.add("--command")
            args.add(state.customCommandName.trim())
        }

        args.add("-f")
        args.add(diffFilePath)
        args.add("--dir")
        args.add(projectPath)

        if (state.customCommandName.isBlank()) {
            args.add(buildPrompt(state))
        }

        return args
    }

    private fun buildPrompt(state: OpenCodeCommitSettings.State): String {
        val gitPolicy = if (state.allowAdditionalGitCommands) {
            "附件 diff 是主要依據；必要時可以補充查詢 repository context。"
        } else {
            "只使用附件 diff 作為依據；不要執行 git status、git diff、git log 或其他 git 指令。"
        }

        return listOf(
            state.promptTemplate.trim(),
            gitPolicy,
            "只輸出單一 commit message，不要 markdown、不要 code fence、不要解釋文字。",
        ).joinToString("\n")
    }
}
