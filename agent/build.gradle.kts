plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.3.1"
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.9.1")
    shadow("org.ow2.asm:asm:9.9.1")
}

tasks {
    jar {
        manifest {
            attributes(
                "Main-Class" to "net.janrupf.gradle.hytale.dev.agent.HytaleDevAgent",
            )
        }
    }

    shadowJar {
        relocate("org.objectweb.asm", "net.janrupf.gradle.hytale.dev.agent.shaded.org.objectweb.asm")
    }
}
