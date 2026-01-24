# Hytale Dev

Gradle plugin for Hytale plugin development.

Early alpha software, expect bugs and incomplete features. Contributions are welcome!

## Requirements

- Java 24+
- Gradle 8.x or 9.x
- Hytale installed (or manual path configuration)

## Usage

**build.gradle.kts**
```kotlin
plugins {
    // See latest version at https://plugins.gradle.org/plugin/net.janrupf.hytale-dev
    id("net.janrupf.hytale-dev") version "0.1.0"
}

group = "com.example"
version = "1.0.0"

hytale {
    manifest {
        main("com.example.MyPluginMain")
        description("My Hytale plugin")
        author {
            name("Your Name")
        }
    }
}

dependencies {
    compileOnly(hytaleServer()) // Must use compileOnly
}
```

The plugin generates `manifest.json` automatically based on the `manifest {}` configuration.


If you want to install the plugin from source to test changes before they have been published to
the Gradle Plugin Portal, you can use JitPack as a repository:

<details>
<summary>Installation from Jitpack</summary>

**settings.gradle.kts**
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

Then replace the plugin version in `build.gradle.kts` with the desired commit hash or branch name:

```kotlin
plugins {
    id("net.janrupf.hytale-dev") version "<commit-hash-or-branch>"
}
```

</details>



## Configuration Reference

### Extension Properties (`hytale {}`)

See `HytaleExtension` class for implementation details.

| Property                   | Default                                | Description                      |
|----------------------------|----------------------------------------|----------------------------------|
| `serverJar`                | Auto-detected from Hytale installation | Path to HytaleServer.jar         |
| `hytaleAssetsZip`          | Auto-detected from Hytale installation | Path to Assets.zip               |
| `enableDecompileServerJar` | `true`                                 | Decompile server for IDE sources |

### Manifest Configuration (`manifest {}`)

See `HytaleManifestModel` class for implementation details.

| Method                                     | Default             | Description                                  |
|--------------------------------------------|---------------------|----------------------------------------------|
| `main(String)`                             | -                   | Main plugin class (required)                 |
| `identifier(name, group)`                  | Project name/group  | Plugin identifier                            |
| `version(String)`                          | Project version     | Plugin version                               |
| `description(String)`                      | Project description | Plugin description                           |
| `author { }`                               | Empty list          | Add author with `name()`, `email()`, `url()` |
| `website(String)`                          | Not set             | Plugin website URL                           |
| `serverVersion(String)`                    | `"*"`               | Server version requirement                   |
| `dependency(group, name, version)`         | Empty               | Required dependency                          |
| `optionalDependency(group, name, version)` | Empty               | Optional dependency                          |
| `loadBefore(group, name, version)`         | Empty               | Plugins that should load after this one      |
| `disableByDefault(Boolean)`                | `false`             | Whether plugin is disabled by default        |
| `includesAssetPack(Boolean)`               | `true`              | Whether plugin includes an asset pack        |

### Run Configurations (`runs {}`)

A default `server` run configuration is created automatically. See `HytaleRunModel` class for implementation details.

| Property/Method           | Default                   | Description                    |
|---------------------------|---------------------------|--------------------------------|
| `workingDirectory`        | `run/<name>`              | Working directory              |
| `arg(String)`             | -                         | Add server argument            |
| `jvmArg(String)`          | -                         | Add JVM argument               |
| `environment(key, value)` | -                         | Set environment variable       |
| `serverJar`               | From extension            | Override server JAR            |
| `assetsZip`               | From extension            | Override assets ZIP            |
| `allowOp`                 | `true`                    | Allow operator commands        |
| `enabled`                 | `true`                    | Enable/disable this run config |
| `mainClassName`           | `com.hypixel.hytale.Main` | Main class to launch           |

**Example:**
```kotlin
hytale {
    runs {
        getByName("server") {
            jvmArg("-Xmx4G")
        }
        create("debug") {
            // Not required for debugging, just an example
            jvmArg("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005")
        }
    }
}
```

## Tasks

| Task                     | Description                          |
|--------------------------|--------------------------------------|
| `runServer`              | Run the default server configuration |
| `run<Name>`              | Run a custom run configuration       |
| `generatePluginManifest` | Generate manifest.json               |
| `decompileHytaleServer`  | Decompile server JAR for IDE sources |
| `importHytaleServerJar`  | Import server JAR to build           |

## Hytale Installation Detection

The plugin auto-detects Hytale from standard installation paths:

- **Windows**: `%APPDATA%\Hytale\...`
- **macOS**: `~/Library/Application Support/Hytale/...`
- **Linux**: `$XDG_DATA_HOME/Hytale/...` or `~/.local/share/Hytale/...`
- **Flatpak**: `~/.var/app/com.hypixel.HytaleLauncher/data/Hytale/...`

Override with project property:
```
./gradlew build -Phytale.gamePackageDir=/path/to/game/package
```

## License

GPL-3.0 - see [LICENSE](LICENSE)
