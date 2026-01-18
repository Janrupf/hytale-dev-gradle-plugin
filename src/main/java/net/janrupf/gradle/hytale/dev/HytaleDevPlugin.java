package net.janrupf.gradle.hytale.dev;

import net.janrupf.gradle.hytale.dev.agent.HytaleDevAgentConfiguration;
import net.janrupf.gradle.hytale.dev.extension.HytaleServerDependencyExtension;
import net.janrupf.gradle.hytale.dev.extension.HytaleExtension;
import net.janrupf.gradle.hytale.dev.ide.IdeIntegration;
import net.janrupf.gradle.hytale.dev.repository.HytaleServerRepository;
import net.janrupf.gradle.hytale.dev.run.RunGenerator;
import net.janrupf.gradle.hytale.dev.tasks.ExtractAgentTask;
import net.janrupf.gradle.hytale.dev.tasks.GenerateHytaleManifestTask;
import net.janrupf.gradle.hytale.dev.tasks.SingleFileCopyTask;
import net.janrupf.gradle.hytale.dev.tasks.VineflowerDecompileTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.util.Collections;

public abstract class HytaleDevPlugin implements Plugin<Project> {
    public static final String HYTALE_TASK_GROUP = "hytale";
    public static final String HYTALE_LOCAL_REPOSITORY_GROUP = "net.janrupf.gradle.hytale.local";

    private Project project;
    private IdeIntegration ideIntegration;
    private HytaleExtension extension;

    private HytaleDevAgentConfiguration agentConfiguration;
    private RunGenerator runGenerator;
    private HytaleServerRepository hytaleServerRepository;

    private TaskProvider<GenerateHytaleManifestTask> generateManifestTask;

    private Provider<RegularFile> importedHytaleServerJar;
    private Configuration vineflowerConfiguration;
    private Configuration jacksonConfiguration;

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);

        this.project = project;
        this.ideIntegration = IdeIntegration.autoDetect(project);
        this.extension = project.getExtensions().create("hytale", HytaleExtension.class);

        var extractAgentTask = project.getTasks().register("extractAgent", ExtractAgentTask.class, (task) -> {
            task.getTargetFile().convention(project.getLayout().getBuildDirectory().file("hytale-dev/agent.jar"));
        });

        var importHytaleServerJarTask = project.getTasks().register("importHytaleServerJar", SingleFileCopyTask.class, (task) -> {
            task.getInputFile().convention(extension.getServerJar());
            task.getOutputFile().convention(hytaleServerRepository.getEntry("HytaleServer.jar").getFile());
        });
        this.ideIntegration.runTaskOnSync(importHytaleServerJarTask);
        this.importedHytaleServerJar = importHytaleServerJarTask.flatMap(SingleFileCopyTask::getOutputFile);

        this.generateManifestTask = project.getTasks().register(
                "generatePluginManifest",
                GenerateHytaleManifestTask.class,
                this::configureGenerateManifestTask
        );

        this.agentConfiguration = new HytaleDevAgentConfiguration(
                extractAgentTask.flatMap(ExtractAgentTask::getTargetFile),
                importedHytaleServerJar
        );

        var mainSourceSet = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        var mainProcessResourcesTask = project.getTasks().named(mainSourceSet.getProcessResourcesTaskName(), Copy.class, this::configureProcessResourcesTask);

        this.runGenerator = new RunGenerator(
                project,
                ideIntegration,
                project.getLayout().dir(mainProcessResourcesTask.map(Copy::getDestinationDir)),
                agentConfiguration
        );

        this.hytaleServerRepository = new HytaleServerRepository(project);

        var dependencies = project.getDependencies();
        this.vineflowerConfiguration = project.getConfigurations().detachedConfiguration(dependencies.create("org.vineflower:vineflower:1.11.2"));
        this.jacksonConfiguration = project.getConfigurations().detachedConfiguration(
                dependencies.create("tools.jackson.core:jackson-core:3.0.3"),
                dependencies.create("tools.jackson.core:jackson-databind:3.0.3")
        );

        this.ideIntegration.runTaskOnSync(project.getTasks().register("decompileHytaleServer", VineflowerDecompileTask.class, this::configureDecompileHytaleServerTask));

        project.getRepositories().flatDir(this::configureHytaleInstallationAsFlatDirRepository);
        project.getDependencies().getExtensions().create(
                "hytaleServer",
                HytaleServerDependencyExtension.class,
                project,
                importedHytaleServerJar
        );

        this.extension.getRuns().all(this.runGenerator::generate);


        project.afterEvaluate(this::afterEvaluate);
    }

    private void configureGenerateManifestTask(GenerateHytaleManifestTask task) {
        task.setGroup(HYTALE_TASK_GROUP);
        task.setDescription("Generates a Hytale plugin manifest file");

        task.getTarget().convention(project.getLayout().getBuildDirectory().dir("hytale-dev").map(
                (dir) -> dir.dir("manifest").file("manifest.json")));
        task.getManifest().convention(project.provider(() -> {
            var manifest = extension.getManifest().get();
            var serialized = manifest.toMap().get();

            for (var modifier : extension.getManifestModifiers().get()) {
                modifier.execute(serialized);
            }

            return serialized;
        }));
        task.getJacksonClasspath().setFrom(jacksonConfiguration);
    }

    private void configureHytaleInstallationAsFlatDirRepository(FlatDirectoryArtifactRepository repository) {
        repository.setName("LocalHytaleServerRepository");
        repository.dir(hytaleServerRepository.getRepositoryDir());
        repository.content((content) -> content.includeGroup(HYTALE_LOCAL_REPOSITORY_GROUP));
    }

    private void configureDecompileHytaleServerTask(VineflowerDecompileTask task) {
        task.setGroup(HYTALE_TASK_GROUP);
        task.setDescription("Decompile the Hytale server jar using Vineflower");

        task.getInputJar().convention(importedHytaleServerJar);
        task.getDecompiledOutputJar().convention(hytaleServerRepository.getEntry("HytaleServer-sources.jar").getFile());
        task.getPrefixes().convention(Collections.singleton("com/hypixel"));
        task.getVineflowerClasspath().setFrom(vineflowerConfiguration);
        task.setEnabled(extension.getEnableDecompileServerJar().get());
    }

    private void configureProcessResourcesTask(Copy task) {
        task.from(this.generateManifestTask);
    }

    private void afterEvaluate(Project project) {
        ideIntegration.finalizeConfiguration();
    }
}
