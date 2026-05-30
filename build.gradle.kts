plugins {
    id("org.jetbrains.intellij.platform")
    kotlin("jvm") version "2.0.21"
}

group = "com.github.opencode"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

dependencies {
    intellijPlatform {
        rider("2024.3.1")
    }
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

tasks.test {
    useJUnitPlatform()
}
