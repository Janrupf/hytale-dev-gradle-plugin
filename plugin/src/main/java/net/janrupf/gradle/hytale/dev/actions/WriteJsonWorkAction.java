package net.janrupf.gradle.hytale.dev.actions;

import org.gradle.workers.WorkAction;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

public abstract class WriteJsonWorkAction implements WorkAction<WriteJsonWorkActionParams> {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void execute() {
        var params = getParameters();
        var target = params.getTarget().get().getAsFile().toPath();

        if (target.getParent() != null) {
            try {
                Files.createDirectories(target.getParent());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        if (params.getPrettyPrint().getOrElse(false)) {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(target, params.getValue().get());
            return;
        }

        MAPPER.writeValue(target, params.getValue().get());
    }
}
