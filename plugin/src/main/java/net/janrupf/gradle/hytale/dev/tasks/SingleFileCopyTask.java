package net.janrupf.gradle.hytale.dev.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Path insensitive, single file version of a copy task.
 */
@CacheableTask
public abstract class SingleFileCopyTask extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputFile();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void copy() throws IOException {
        var inputFilePath = getInputFile().get().getAsFile().toPath();
        var outputFilePath = getOutputFile().get().getAsFile().toPath();

        var outputParent = outputFilePath.getParent();
        if (outputParent == null) {
            Files.createDirectories(outputFilePath);
        }

        Files.copy(inputFilePath, outputFilePath, StandardCopyOption.REPLACE_EXISTING);
    }
}
