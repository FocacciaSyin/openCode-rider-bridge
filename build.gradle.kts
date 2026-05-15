plugins {
    id("org.jetbrains.intellij.platform")
}

group = "com.github.opencode"
version = "0.1.0"

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

intellijPlatform {
    pluginConfiguration {
        id = "com.github.opencode.rider-context"
        name = "OpenCode Rider Context"
        version = project.version.toString()
        description = "Publishes Rider editor selection to OpenCode through the Claude Code IDE bridge protocol."
        ideaVersion {
            sinceBuild = "243"
            untilBuild = "261.*"
        }
    }
}
