package net.janrupf.gradle.hytale.dev.repository;

import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

public class HytaleServerRepositoryEntry {
    private final Provider<RegularFile> file;

    public HytaleServerRepositoryEntry(Provider<RegularFile> file) {
        this.file = file;
    }

    public Provider<RegularFile> getFile() {
        return file;
    }
}
