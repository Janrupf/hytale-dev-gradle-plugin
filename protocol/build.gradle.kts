plugins {
    java
    id("com.google.protobuf") version "0.9.4"
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.25.1")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
}

// Ensure generated sources are included in the source set
sourceSets {
    main {
        java {
            srcDirs(
                layout.buildDirectory.dir("generated/source/proto/main/java")
            )
        }
    }
}
