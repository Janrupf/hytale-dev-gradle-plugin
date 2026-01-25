package net.janrupf.gradle.hytale.dev.extension;

import org.gradle.api.Project;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

public abstract class HytaleServerDependencyExtension {
    private final Project project;
    private final DependencyFactory dependencyFactory;
    private final Provider<RegularFile> importedHytaleServerJar;

    public HytaleServerDependencyExtension(Project project, Provider<RegularFile> importedHytaleServerJar) {
        this.project = project;
        this.dependencyFactory = project.getDependencyFactory();
        this.importedHytaleServerJar = importedHytaleServerJar;
    }

    public FileCollectionDependency invoke() {
        return dependencyFactory.create(project.files(importedHytaleServerJar));
    }
}
