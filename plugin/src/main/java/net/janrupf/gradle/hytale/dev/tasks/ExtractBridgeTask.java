package net.janrupf.gradle.hytale.dev.tasks;

import net.janrupf.gradle.hytale.dev.HytaleDevPlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Files;

/**
 * Task that extracts the bundled bridge JAR from the plugin resources.
 */
@CacheableTask
public abstract class ExtractBridgeTask extends DefaultTask {
    @OutputFile
    public abstract RegularFileProperty getTargetFile();

    @TaskAction
    public void extract() throws Exception {
        try (var packagedBridge = HytaleDevPlugin.class.getResourceAsStream("/hytale-dev/bridge/bridge.jar")) {
            if (packagedBridge == null) {
                throw new IllegalStateException("Could not find packaged bridge.jar");
            }

            var targetPath = getTargetFile().get().getAsFile().toPath();

            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            Files.copy(packagedBridge, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
