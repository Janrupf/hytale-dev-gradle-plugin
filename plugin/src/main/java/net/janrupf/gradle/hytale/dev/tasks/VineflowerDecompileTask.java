package net.janrupf.gradle.hytale.dev.tasks;

import net.janrupf.gradle.hytale.dev.actions.VineflowerDecompilerWorkActionParams;
import net.janrupf.gradle.hytale.dev.actions.VineflowerDecompilerWorkAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

@CacheableTask
public abstract class VineflowerDecompileTask extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInputJar();

    @OutputFile
    public abstract RegularFileProperty getDecompiledOutputJar();

    @Input
    public abstract MapProperty<String, Object> getVineflowerPreferences();

    @Input
    public abstract ListProperty<String> getPrefixes();

    @Input
    public abstract Property<String> getMaxHeapSize();

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getVineflowerClasspath();

    @Inject
    public VineflowerDecompileTask() {
        getVineflowerPreferences().convention(VineflowerDecompilerWorkActionParams.defaultPreferencesProvider(getProject()));
        getMaxHeapSize().convention("4G");
    }

    @TaskAction
    public void run() {
        var queue = getWorkerExecutor().processIsolation((spec) -> {
            spec.getClasspath().from(getVineflowerClasspath());
            spec.forkOptions((options) -> options.setMaxHeapSize(getMaxHeapSize().get()));
        });

        queue.submit(VineflowerDecompilerWorkAction.class, (params) -> {;
            params.getInputJar().set(getInputJar());
            params.getDecompiledOutputJar().set(getDecompiledOutputJar());
            params.getVineflowerPreferences().set(getVineflowerPreferences());
            params.getPrefixes().set(getPrefixes());
        });
    }
}
