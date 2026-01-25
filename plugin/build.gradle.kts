plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = rootProject.group
version = rootProject.version

val agentProject = project(":agent")
val bridgeProject = project(":bridge")

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.processResources {
    from(agentProject.tasks.named("shadowJar")) {
        into("hytale-dev/agent")
        rename(".*", "agent.jar")
    }
    from(bridgeProject.tasks.named("shadowJar")) {
        into("hytale-dev/bridge")
        rename(".*", "bridge.jar")
    }
}

dependencies {
    // Used by the VineflowerDecompilerTask, keep in sync with the version in HytaleDevPlugin
    compileOnly("org.vineflower:vineflower:1.11.2")

    // Used by WriteJsonWorkAction, keep in sync with the version in HytaleDevPlugin
    compileOnly("tools.jackson.core:jackson-core:3.0.3")
    compileOnly("tools.jackson.core:jackson-databind:3.0.3")

    // For generating IDE run configurations
    implementation("org.jetbrains.gradle.plugin.idea-ext:org.jetbrains.gradle.plugin.idea-ext.gradle.plugin:1.3")
}

gradlePlugin {
    website.set("https://github.com/Janrupf/hytale-dev-gradle-plugin")
    vcsUrl.set("https://github.com/Janrupf/hytale-dev-gradle-plugin.git")
    plugins {
        create("hytaleDev") {
            id = "net.janrupf.hytale-dev"
            implementationClass = "net.janrupf.gradle.hytale.dev.HytaleDevPlugin"
            displayName = "Hytale Dev"
            description = "A Gradle plugin for Hytale mod/plugin development."
            tags.set(listOf("hytale", "modding", "development"))
        }
    }
}
