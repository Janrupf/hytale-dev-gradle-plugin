import java.io.File

plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

// Find Hytale server JAR for compilation
fun findServerJar(): File? {
    val gamePackageDir: File? = System.getProperty("hytale.gamePackageDir")?.let { path ->
        File(path).takeIf { it.isDirectory }
    } ?: run {
        val userHome = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()

        val dataHome: File = when {
            os.contains("mac") -> File(userHome, "Library/Application Support")
            os.contains("win") -> System.getenv("APPDATA")?.let { File(it) }
                ?: File(userHome, "AppData/Roaming")
            else -> System.getenv("XDG_DATA_HOME")?.let { File(it) }
                ?: File(userHome, ".local/share")
        }

        val standardPath = File(dataHome, "Hytale/install/release/package/game/latest")
        if (standardPath.isDirectory) {
            standardPath
        } else if (!os.contains("win") && !os.contains("mac")) {
            // Linux Flatpak fallback
            File(userHome, ".var/app/com.hypixel.HytaleLauncher/data/Hytale/install/release/package/game/latest")
                .takeIf { it.isDirectory }
        } else {
            null
        }
    }

    return gamePackageDir?.let { File(it, "Server/HytaleServer.jar") }
        ?.takeIf { it.isFile }
}

val serverJar = findServerJar()

dependencies {
    implementation(project(":protocol"))
    implementation("org.java-websocket:Java-WebSocket:1.5.4")

    if (serverJar != null) {
        compileOnly(files(serverJar))
    } else {
        logger.warn("""
            |
            |WARNING: HytaleServer.jar not found!
            |The bridge module will not compile server-dependent code.
            |
            |To fix this, either:
            |  - Install Hytale via the official launcher
            |  - Set -Dhytale.gamePackageDir=/path/to/game/package
            |
            |Expected locations:
            |  Windows: %APPDATA%/Hytale/install/release/package/game/latest
            |  macOS: ~/Library/Application Support/Hytale/install/release/package/game/latest
            |  Linux: ~/.local/share/Hytale/install/release/package/game/latest
            |  Flatpak: ~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/install/release/package/game/latest
            |
        """.trimMargin())
    }
}

tasks.shadowJar {
    // Relocate dependencies to avoid conflicts with server or other plugins
    relocate("com.google.protobuf", "net.janrupf.gradle.hytale.dev.shadow.protobuf")
    relocate("org.java_websocket", "net.janrupf.gradle.hytale.dev.shadow.websocket")

    // Archive configuration
    archiveClassifier.set("")
    archiveBaseName.set("hytale-dev-bridge")

    // Merge service files for plugin discovery
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
