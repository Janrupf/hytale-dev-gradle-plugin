package net.janrupf.gradle.hytale.dev.tasks;

import net.janrupf.gradle.hytale.dev.actions.WriteJsonWorkAction;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.*;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

@CacheableTask
public abstract class GenerateHytaleManifestTask extends DefaultTask {
    @Input
    public abstract MapProperty<String, Object> getManifest();

    @OutputFile
    public abstract RegularFileProperty getTarget();

    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getJacksonClasspath();

    @Inject
    public abstract WorkerExecutor getWorkerExecutor();

    @TaskAction
    public void generate() {
        getWorkerExecutor().classLoaderIsolation(
                (classLoader) -> classLoader.getClasspath().setFrom(getJacksonClasspath())
        ).submit(WriteJsonWorkAction.class, (params) -> {
            params.getValue().set(getManifest().get());
            params.getTarget().set(getTarget());
            params.getPrettyPrint().set(true);
        });
    }
}
