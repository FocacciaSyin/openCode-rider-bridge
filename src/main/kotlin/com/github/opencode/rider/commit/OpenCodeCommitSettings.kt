package com.github.opencode.rider.commit

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "OpenCodeCommitSettings", storages = [Storage("opencode-commit-message.xml")])
class OpenCodeCommitSettings : PersistentStateComponent<OpenCodeCommitSettings.State> {
    private var currentState = State()

    override fun getState(): State = currentState

    override fun loadState(state: State) {
        currentState = state.copy()
    }

    data class State(
        var opencodePath: String = "opencode",
        var model: String = "",
        var customCommandName: String = "",
        var promptTemplate: String = DEFAULT_PROMPT_TEMPLATE,
        var allowAdditionalGitCommands: Boolean = false,
        var timeoutSeconds: Int = 60,
        var diffSizeLimitBytes: Int = 1_048_576,
        var enabledByDefault: Boolean = false,
    )

    companion object {
        const val DEFAULT_PROMPT_TEMPLATE: String =
            "請根據附件 diff 產生一個 commit message，只輸出 message，不要 markdown、不要解釋。"
    }
}
