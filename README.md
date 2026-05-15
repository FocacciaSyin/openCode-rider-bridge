# OpenCode Rider Context

這是一個 JetBrains Rider 外掛，用來把 Rider 目前開啟的檔案與選取範圍送給 OpenCode TUI。

目前已在 OpenCode `1.15.0` 與 Rider `RD-261.23567.144` 驗證可用。

它使用 OpenCode 已支援的 Claude Code IDE bridge 相容協議，所以目標是不改 OpenCode，只補上 Rider 端的 bridge。

## 運作方式

外掛啟動後會開一個 localhost WebSocket server，並寫入 lock file：

```txt
~/.claude/ide/<port>.lock
```

lock file 內容類似：

```json
{
  "transport": "ws",
  "authToken": "generated-token",
  "workspaceFolders": ["/absolute/path/to/project"]
}
```

OpenCode TUI 會掃描 `~/.claude/ide`。如果目前執行 `opencode` 的目錄落在 `workspaceFolders` 裡，OpenCode 會連到這個 WebSocket server，接收 Rider 推送的 `selection_changed` JSON-RPC notification。

資料流：

```txt
Rider plugin
  -> ~/.claude/ide/<port>.lock
  -> OpenCode TUI 連線
  -> Rider selection_changed
  -> OpenCode prompt 自動帶入選取內容
```

## 專案進入點

JetBrains 外掛沒有一般 Java 程式常見的 `main()`。這個專案的啟動入口是：

```txt
src/main/resources/META-INF/plugin.xml
```

`plugin.xml` 會把 project service 與 startup activity 註冊給 Rider：

```xml
<projectService serviceImplementation="com.github.opencode.rider.OpencodeContextProjectService" />
<postStartupActivity implementation="com.github.opencode.rider.OpencodeContextStartupActivity" />
```

啟動鏈如下：

```txt
plugin.xml
  -> OpencodeContextStartupActivity.runActivity()
  -> OpencodeContextProjectService.start()
  -> ClaudeIdeBridgeServer.start()
  -> ClaudeIdeLockFile.create()
  -> registerEditorListeners()
  -> publishSelection()
```

如果你剛接手這個專案，建議先從這三個檔案看起：

- `src/main/resources/META-INF/plugin.xml`：Rider 載入外掛時讀取的註冊檔，定義外掛啟動後要建立哪個 service、執行哪個 startup activity。
- `OpencodeContextStartupActivity.java`：Rider 專案啟動後的第一段 Java 程式碼，只負責取得 `OpencodeContextProjectService` 並呼叫 `start()`。
- `OpencodeContextProjectService.java`：主要邏輯集中在這裡，負責啟動 WebSocket server、寫 lock file、監聽 editor selection/caret，並把選取內容送給 OpenCode。

除錯時可以優先在這些方法下 breakpoint：

- `OpencodeContextStartupActivity.runActivity()`：確認 Rider 是否有載入並執行這個外掛。
- `OpencodeContextProjectService.start()`：確認 server、port、auth token、lock file 是否成功建立。
- `ClaudeIdeBridgeServer.onOpen()`：確認 OpenCode 是否有連上 Rider 端 WebSocket server。
- `ClaudeIdeBridgeServer.onMessage()`：確認 OpenCode 是否有送出 JSON-RPC `initialize` request。
- `OpencodeContextProjectService.publishSelection()`：確認 Rider 是否有讀到目前 editor、檔案路徑與選取文字。
- `SelectionPayload.toJsonRpcNotification()`：確認送給 OpenCode 的 `selection_changed` JSON-RPC notification 格式是否正確。

## Gradle Kotlin DSL 檔案

這個 repo 根目錄有兩個 `.kts` 檔案：

```txt
settings.gradle.kts
build.gradle.kts
```

`.kts` 是 Kotlin Script。這裡的 `.gradle.kts` 是 Gradle build 設定檔，只是使用 Kotlin DSL 撰寫，不是 Rider plugin 的執行入口，也不是產品邏輯本身。

`settings.gradle.kts` 負責 Gradle 專案層級設定，包含：

- 設定 Gradle plugin 下載來源。
- 設定 dependencies 下載來源。
- 啟用 JetBrains IntelliJ Platform Gradle settings plugin。
- 設定 root project 名稱：`rider-opencode-context`。

目前內容重點如下：

```kotlin
plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.2.1"
}

rootProject.name = "rider-opencode-context"
```

`build.gradle.kts` 負責實際 build 與 JetBrains plugin 打包設定，包含：

- 套用 `org.jetbrains.intellij.platform` Gradle plugin。
- 設定專案 `group` 與 `version`。
- 設定 Java toolchain 使用 JDK 21。
- 指定 Rider SDK 版本：`2024.3.1`。
- 加入 WebSocket dependency：`org.java-websocket:Java-WebSocket:1.5.7`。
- 設定 JetBrains plugin metadata，例如 plugin id、name、description、相容 Rider build 範圍。

目前內容重點如下：

```kotlin
plugins {
    id("org.jetbrains.intellij.platform")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    intellijPlatform {
        rider("2024.3.1")
    }
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
}
```

簡單分工如下：

```txt
settings.gradle.kts = Gradle 專案與 repository 設定
build.gradle.kts    = build、dependency、Rider SDK、plugin metadata 設定
plugin.xml          = JetBrains plugin 註冊入口
src/main/java       = plugin 實際 Java 程式碼
```

## 這個專案如何達成目標

這個專案沒有修改 OpenCode 本身，而是在 Rider 裡實作一個 Claude Code IDE bridge 相容層。OpenCode 已經支援讀取 `~/.claude/ide/*.lock` 並連線到 IDE bridge，所以這個外掛只要在 Rider 端提供相同協議，就能讓 OpenCode 把 Rider 選取內容當成 editor context 使用。

核心流程如下：

1. Rider 載入外掛後，`OpencodeContextStartupActivity` 會在專案啟動時取得 `OpencodeContextProjectService` 並呼叫 `start()`。
2. `OpencodeContextProjectService` 會取得目前 Rider 專案的 `basePath`，用 `PortAllocator` 找一個可用的 localhost port，並產生一個隨機 `authToken`。
3. `ClaudeIdeBridgeServer` 會在 `127.0.0.1:<port>` 啟動 WebSocket server，只接受帶有正確 `x-claude-code-ide-authorization` header 的連線。
4. `ClaudeIdeLockFile` 會寫入 `~/.claude/ide/<port>.lock`，內容包含 `transport`、`authToken` 與 `workspaceFolders`。
5. OpenCode TUI 啟動時會掃描 `~/.claude/ide`，讀取 lock file，並檢查目前執行 `opencode` 的資料夾是否位於某個 `workspaceFolders` 底下。
6. 如果資料夾匹配，OpenCode 會用 lock file 裡的 port 與 auth token 連到 Rider 端 WebSocket server。
7. OpenCode 連線後會送出 JSON-RPC `initialize` request，`ClaudeIdeBridgeServer` 會回傳 `protocolVersion` 與 `serverInfo`。
8. Rider 中的 caret 或 selection 改變時，`OpencodeContextProjectService` 會從目前 editor 讀取檔案路徑、選取文字、起訖位置，組成 `SelectionPayload`。
9. `SelectionPayload` 會輸出 OpenCode 可解析的 JSON-RPC `selection_changed` notification。
10. OpenCode 收到 `selection_changed` 後，就會把 Rider 目前選取內容放進 editor context，後續提問時可以直接引用。

主要檔案分工：

- `src/main/resources/META-INF/plugin.xml`：註冊 Rider project service 與 startup activity。
- `OpencodeContextStartupActivity.java`：專案啟動時啟動 bridge service。
- `OpencodeContextProjectService.java`：整合 Rider editor listener、WebSocket server、lock file 生命週期與 selection 發送。
- `PortAllocator.java`：取得可用 localhost port。
- `ClaudeIdeLockFile.java`：建立與清除 `~/.claude/ide/<port>.lock`。
- `ClaudeIdeLockFilePayload.java`：產生 OpenCode 可讀的 lock file JSON。
- `ClaudeIdeBridgeServer.java`：實作 Claude Code IDE bridge 相容 WebSocket server。
- `ClaudeIdeProtocol.java`：處理 JSON-RPC initialize response。
- `SelectionPayload.java`：把 Rider editor selection 轉成 `selection_changed` notification。
- `JsonString.java`：處理 JSON 字串 escape，避免路徑或選取文字破壞 JSON 格式。

這個設計的重點是「讓 Rider 看起來像 OpenCode 已支援的 IDE bridge」。因此 OpenCode 端不需要額外 plugin，也不需要改 source code；只要它能找到 lock file 並連上 WebSocket，就可以取得 Rider 的目前選取內容。

## 你需要先準備什麼

建議安裝：

- JDK 21
- Rider
- OpenCode CLI

確認 Java 版本：

```powershell
java -version
```

如果 Gradle 顯示 `JAVA_HOME is set to an invalid directory`，請確認 `JAVA_HOME` 指到 JDK 根目錄，而且底下有 `bin\java.exe`。

Windows 範例：

```powershell
setx JAVA_HOME "C:\Program Files\Java\jdk-21.0.11"
```

重新開啟 terminal 後確認：

```powershell
$env:JAVA_HOME
Test-Path "$env:JAVA_HOME\bin\java.exe"
java -version
```

## 第一次要執行的指令

進入這個 repo：

```powershell
cd D:\Github\rider-opencode-context
```

如果你已經有安裝全域 Gradle，先產生 Gradle wrapper：

```powershell
gradle wrapper
```

之後都用 repo 內的 wrapper：

```powershell
.\gradlew --version
```

如果你沒有全域 Gradle，可以先用 Rider 或 IntelliJ IDEA 打開 `D:\Github\rider-opencode-context`，讓 IDE 匯入 Gradle 專案，再從 IDE 的 Gradle tool window 執行 task。

## Development

## Rider 相容版本

目前打包出的插件 metadata 設定為：

```xml
<idea-version since-build="243" until-build="261.*" />
```

因此可安裝到 Rider `RD-261.*`。如果 Rider 顯示：

```txt
Plugin 'OpenCode Rider Context' is not compatible with the current version of the IDE,
because it requires build 243.* or older
```

代表你拿到的是舊 zip。請重新執行：

```powershell
.\gradlew buildPlugin
```

然後重新安裝：

```txt
D:\Github\rider-opencode-context\build\distributions\rider-opencode-context-0.1.0.zip
```

## 跑核心協議測試

這組測試不需要 JetBrains SDK，也不需要 Gradle。它只驗證 JSON protocol、lock file 內容、selection payload 格式。

```powershell
javac -d "build\test-classes" "src\main\java\com\github\opencode\rider\JsonString.java" "src\main\java\com\github\opencode\rider\ClaudeIdeLockFilePayload.java" "src\main\java\com\github\opencode\rider\ClaudeIdeProtocol.java" "src\main\java\com\github\opencode\rider\SelectionPayload.java" "src\main\java\com\github\opencode\rider\ClaudeIdeLockFile.java" "src\main\java\com\github\opencode\rider\PortAllocator.java" "src\test\java\com\github\opencode\rider\ProtocolTests.java" "src\test\java\com\github\opencode\rider\LockFileTests.java"
java -cp "build\test-classes" com.github.opencode.rider.ProtocolTests
java -cp "build\test-classes" com.github.opencode.rider.LockFileTests
```

## 跑 Rider sandbox

開發外掛時通常不需要先打包。你可以直接跑 sandbox IDE：

```powershell
.\gradlew runIde
```

這會啟動一個測試用 Rider/JetBrains IDE，並載入目前外掛。第一次執行會下載 JetBrains SDK，時間可能比較久。

如果你是從 IDE 的 Gradle tool window 操作，執行 `runIde` task 即可。

## 需要打包嗎？

開發與測試階段：不需要打包，使用 `runIde`。

要安裝到你平常使用的 Rider：需要打包。

打包指令：

```powershell
.\gradlew buildPlugin
```

打包完成後，zip 會在：

```txt
build/distributions/rider-opencode-context-0.1.0.zip
```

然後到 Rider：

```txt
Settings -> Plugins -> Install Plugin from Disk...
```

選擇 `build/distributions/rider-opencode-context-0.1.0.zip`。

## 手動驗證流程

1. 用 `runIde` 開啟 sandbox Rider，或把 zip 安裝到 Rider。
2. 在 Rider 開一個專案，例如 `D:\Github\my-csharp-project`。
3. 確認 lock file 有出現：

```powershell
Get-ChildItem "$env:USERPROFILE\.claude\ide"
```

4. 到同一個專案目錄啟動 OpenCode：

```powershell
cd D:\Github\my-csharp-project
opencode
```

5. 在 Rider 選一段程式碼。
6. 在 OpenCode TUI 問：

```txt
我剛剛在 Rider 選到的程式碼在做什麼？
```

如果成功，OpenCode 會用 Rider 選取內容回答。

已驗證成功的使用方式：Rider 裝好插件並重啟後，在 Rider 選取程式碼，OpenCode TUI 可以讀到目前選取內容。

## 常用指令整理

```powershell
# 進入 repo
cd D:\Github\rider-opencode-context

# 產生 Gradle wrapper，只需要做一次，需要本機已有 gradle
gradle wrapper

# 跑 sandbox IDE，不需要打包
.\gradlew runIde

# 打包成可安裝 zip
.\gradlew buildPlugin

# 可安裝 zip
build\distributions\rider-opencode-context-0.1.0.zip

# 確認 lock file
Get-ChildItem "$env:USERPROFILE\.claude\ide"
```

## 目前注意事項

- `opencode` 必須從 Rider 專案目錄或其子目錄啟動，OpenCode 才會匹配 `workspaceFolders`。
- 這個外掛是 Claude Code IDE bridge 相容層，不是 JetBrains ACP。
- `runIde` 是開發測試用；`buildPlugin` 才是產生正式安裝 zip。
- 如果安裝過舊版 zip，請先在 Rider 的 Plugins 頁面移除舊版，重啟 Rider，再安裝新版 zip。
