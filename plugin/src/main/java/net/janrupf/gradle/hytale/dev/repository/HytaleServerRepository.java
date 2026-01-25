package net.janrupf.gradle.hytale.dev.repository;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;

public class HytaleServerRepository {
    private final Project project;
    private final Provider<Directory> repositoryDir;

    public HytaleServerRepository(Project project) {
        this.project = project;
        this.repositoryDir = this.project.getLayout().getBuildDirectory()
                .dir("hytale-dev").map((dir) -> dir.dir("server-repository"));
    }

    public Provider<Directory> getRepositoryDir() {
        return repositoryDir;
    }

    public HytaleServerRepositoryEntry getEntry(String filePath) {
        return new HytaleServerRepositoryEntry(repositoryDir.map((dir) -> dir.file(filePath)));
    }
}
