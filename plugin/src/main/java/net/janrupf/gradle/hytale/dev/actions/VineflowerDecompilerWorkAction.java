package net.janrupf.gradle.hytale.dev.actions;

import org.gradle.workers.WorkAction;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver;

public abstract class VineflowerDecompilerWorkAction implements WorkAction<VineflowerDecompilerWorkActionParams> {
    @Override
    public void execute() {
        var params = getParameters();
        var outputFile = params.getDecompiledOutputJar().getAsFile().get();

        var decompilerBuilder = new Decompiler.Builder()
                .inputs(params.getInputJar().getAsFile().get())
                .output(new SingleFileSaver(outputFile));

        for (var entry : params.getVineflowerPreferences().get().entrySet()) {
            decompilerBuilder.option(entry.getKey(), entry.getValue());
        }

        decompilerBuilder.allowedPrefixes(params.getPrefixes().get().toArray(new String[0]));

        var decompiler = decompilerBuilder.build();
        decompiler.decompile();

        System.out.println("Decompiled jar written to: " + outputFile.getAbsolutePath());
    }
}
