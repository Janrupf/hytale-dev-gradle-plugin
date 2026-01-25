package net.janrupf.gradle.hytale.dev.tasks;

import net.janrupf.gradle.hytale.dev.HytaleDevPlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;

@CacheableTask
public abstract class ExtractAgentTask extends DefaultTask {
    @OutputFile
    public abstract RegularFileProperty getTargetFile();

    @TaskAction
    public void extract() throws Exception {
        try (var packagedAgent = HytaleDevPlugin.class.getResourceAsStream("/hytale-dev/agent/agent.jar")) {
            if (packagedAgent == null) {
                throw new IllegalStateException("Could not find packaged agent.jar");
            }

            var targetPath = getTargetFile().get().getAsFile().toPath();

            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            Files.copy(packagedAgent, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
