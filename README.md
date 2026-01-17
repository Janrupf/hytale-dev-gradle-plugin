# Hytale Dev

###### Hytale development plugin for Gradle

This Gradle plugin provides tools and tasks to facilitate the development of Hytale mods and plugins.
Early alpha software, expect bugs and incomplete features. Contributions are welcome!

### Usage

The plugin right now is not published to the gradle plugin portal, so you need to do a bit more setup than
usual:

In your `settings.gradle.kts`, add the following:
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://jitpack.io")
        }
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.janrupf.hytale-dev") {
                useModule("com.github.Janrupf:hytale-dev-gradle-plugin:${requested.version}")
            }
        }
    }
}
```

Then as usual, apply the plugin in your `build.gradle.kts`:
```kotlin
plugins {
    id("net.janrupf.hytale-dev") version "" // Place commit hash, tag or branch here
}

hytale {
    // Configure Hytale plugin specifics here
    manifest {
        main("com.example.my.hytale.PluginMainClass")
        author {
            name("Your Name")
        }
    }
}

dependencies {
    compileOnly(hytaleServer()) // Make sure to use compileOnly!
}

```

You don't need to create a `manifest.json` file manually, the plugin will generate it for you based on the configuration
in the `hytale {}` block.
