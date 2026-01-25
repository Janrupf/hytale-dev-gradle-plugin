package net.janrupf.gradle.hytale.dev.ide;

import net.janrupf.gradle.hytale.dev.agent.HytaleDevAgentConfiguration;
import net.janrupf.gradle.hytale.dev.extension.HytaleRunModel;
import net.janrupf.gradle.hytale.dev.run.RunGenerator;
import net.janrupf.gradle.hytale.dev.tasks.PrepareHytaleServerRunTask;
import net.janrupf.gradle.hytale.dev.util.NamingUtil;
import net.janrupf.gradle.hytale.dev.util.StringEscapeUtil;
import org.gradle.TaskExecutionRequest;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.jetbrains.gradle.ext.*;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

public class IDEAIdeIntegration implements IdeIntegration {
    private final boolean isSyncActive;
    private final Project project;

    private final List<TaskProvider<? extends Task>> syncTasks;
    private final List<IdeaHytaleRunConfiguration> runConfigurations;

    public IDEAIdeIntegration(Project project) {
        this.isSyncActive = Boolean.getBoolean("idea.sync.active");
        this.project = project;

        this.syncTasks = new ArrayList<>();
        this.runConfigurations = new ArrayList<>();

        if (!project.getPlugins().hasPlugin(IdeaPlugin.class)) {
            project.getPlugins().apply(IdeaPlugin.class);
        }

        if (!project.getPlugins().hasPlugin(IdeaExtPlugin.class)) {
            project.getPlugins().apply(IdeaExtPlugin.class);
        }
    }

    @Override
    public void runTaskOnSync(TaskProvider<? extends Task> task) {
        this.syncTasks.add(task);
    }

    @Override
    public void addRunConfiguration(
            TaskProvider<PrepareHytaleServerRunTask> prepareTask,
            HytaleRunModel model,
            HytaleDevAgentConfiguration agentConfiguration
    ) {
        this.runConfigurations.add(new IdeaHytaleRunConfiguration(prepareTask, model, agentConfiguration));
    }

    private void hideBridgeSourceSetTask(Task task) {
        task.setEnabled(false);
        task.setGroup(null);
    }

    @Override
    public void finalizeConfiguration() {
        if (isSyncActive) {
            // Hacks hacks hacks... but it works! Shamelessly stolen from
            // the various Minecraft development plugins
            for (var task : syncTasks) {
                project.getGradle().getStartParameter().getTaskRequests().add(
                        new HytaleDevIdeaSyncTaskExecutionRequest(project, task)
                );
            }
        }

        var ideaModel = project.getExtensions().findByType(IdeaModel.class);
        if (ideaModel == null) {
            return;
        }

        var ideaProject = ideaModel.getProject();
        if (!(ideaProject instanceof ExtensionAware extensionAwareIdeaProject)) {
            return;
        }

        var ideaProjectSettings = extensionAwareIdeaProject.getExtensions().findByType(ProjectSettings.class);
        if (!(ideaProjectSettings instanceof ExtensionAware extensionAwareProjectSettings)) {
            return;
        }

        var ideaRunConfigurations = extensionAwareProjectSettings.getExtensions().findByType(RunConfigurationContainer.class);
        if (ideaRunConfigurations == null) {
            return;
        }

        for (var runConfig : runConfigurations) {
            configureIdeaRun(ideaRunConfigurations, runConfig);
        }
    }

    private void configureIdeaRun(
            RunConfigurationContainer ideaRunConfigurations,
            IdeaHytaleRunConfiguration runConfig
    ) {
        var task = runConfig.prepareTask.get();
        if (!task.isEnabled()) {
            return;
        }

        var model = runConfig.model;

        task.dependsOn(runConfig.agentConfiguration.getAgentJar(), runConfig.agentConfiguration.getServerJar());

        ideaRunConfigurations.create(model.getIdeName().get(), JarApplication.class, (ideaRunConfiguration) -> {
            var environment = new HashMap<>(model.getEnvironment().get());
            environment.put(RunGenerator.AGENT_CONFIGURATION_ENV_VARIABLE, task.getAgentConfigurationFile().get().getAsFile().getAbsolutePath());

            ideaRunConfiguration.setWorkingDirectory(model.getWorkingDirectory().get().getAsFile().getAbsolutePath());
            ideaRunConfiguration.setEnvs(environment);
            ideaRunConfiguration.setModuleName(intelliJModuleName(model.getSourceSet().get()));
            ideaRunConfiguration.setJvmArgs(StringEscapeUtil.escapeArgListForIntelliJ(model.getJvmArguments().get()));
            ideaRunConfiguration.setProgramParameters(StringEscapeUtil.escapeArgListForIntelliJ(model.getArguments().get()));
            ideaRunConfiguration.getBeforeRun().create(
                    "Prepare run",
                    GradleTask.class,
                    (gradleTask) -> gradleTask.setTask(task)
            );
            ideaRunConfiguration.setJarPath(runConfig.agentConfiguration.getAgentJar().get().getAsFile().getAbsolutePath());
        });
    }

    private String intelliJModuleName(SourceSet sourceSet) {
        // Yoink: https://github.com/neoforged/ModDevGradle/blob/main/src/main/java/net/neoforged/moddevgradle/internal/IntelliJIntegration.java#L259-L272
        var moduleName = new StringBuilder();
        moduleName.append(project.getRootProject().getName().replace(" ", "_"));
        if (project != project.getRootProject()) {
            moduleName.append(project.getPath().replaceAll(":", "."));
        }
        moduleName.append(".");
        moduleName.append(sourceSet.getName());
        return moduleName.toString();
    }

    record HytaleDevIdeaSyncTaskExecutionRequest(
            Project project,
            TaskProvider<? extends Task> task
    ) implements TaskExecutionRequest {
        @Override
        public @NonNull List<String> getArgs() {
            return Collections.singletonList(task.getName());
        }

        @Override
        public String getProjectPath() {
            return project.getPath();
        }

        @Override
        public File getRootDir() {
            return project.getRootDir();
        }
    }

    record IdeaHytaleRunConfiguration(
            TaskProvider<PrepareHytaleServerRunTask> prepareTask,
            HytaleRunModel model,
            HytaleDevAgentConfiguration agentConfiguration
    ) {
    }
}
