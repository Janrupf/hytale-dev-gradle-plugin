package net.janrupf.gradle.hytale.dev.run;

import net.janrupf.gradle.hytale.dev.HytaleDevPlugin;
import net.janrupf.gradle.hytale.dev.agent.HytaleDevAgentConfiguration;
import net.janrupf.gradle.hytale.dev.extension.HytaleRunModel;
import net.janrupf.gradle.hytale.dev.ide.IdeIntegration;
import net.janrupf.gradle.hytale.dev.tasks.PrepareHytaleServerRunTask;
import net.janrupf.gradle.hytale.dev.util.NamingUtil;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;

public class RunGenerator {
    public static final String AGENT_MAIN_CLASS = "net.janrupf.gradle.hytale.dev.agent.HytaleDevAgent";
    public static final String AGENT_CONFIGURATION_ENV_VARIABLE = "HYTALE_DEV_AGENT_CONFIGURATION";

    private final Project project;
    private final IdeIntegration ideIntegration;
    private final Provider<Directory> manifestDirectory;
    private final HytaleDevAgentConfiguration agentConfiguration;

    public RunGenerator(
            Project project,
            IdeIntegration ideIntegration,
            Provider<Directory> manifestDirectory,
            HytaleDevAgentConfiguration agentConfiguration
    ) {
        this.project = project;
        this.ideIntegration = ideIntegration;
        this.manifestDirectory = manifestDirectory;
        this.agentConfiguration = agentConfiguration;
    }

    public void generate(HytaleRunModel model) {
        var capitalizedName = NamingUtil.capitalizeFirstLetter(model.getName());

        var prepareRunTask = project.getTasks().register(
                "prepareRun" + capitalizedName,
                PrepareHytaleServerRunTask.class,
                (task) -> {
                    task.setGroup(HytaleDevPlugin.HYTALE_TASK_GROUP);
                    task.setDescription("Prepares the execution of the Hytale " + model.getName() + " configuration.");
                    task.getWorkingDirectory().convention(model.getWorkingDirectory());
                    task.getAgentConfigurationFile().set(getAgentConfigurationFile(model));
                    task.getClasspath().from(
                            model.getSourceSet().map(SourceSet::getRuntimeClasspath),
                            agentConfiguration.getServerJar()
                    );
                    task.getMainClassName().set(model.getMainClassName());
                    task.getAssetsRedirectSource().set(manifestDirectory);
                    task.getAssetsRedirectTarget().set(model.getAssetsLocation());
                    task.setEnabled(model.getEnabled().get());
                }
        );

        project.getTasks().register("run" + capitalizedName, JavaExec.class, (task) -> {
            task.dependsOn(prepareRunTask);
            task.setGroup(HytaleDevPlugin.HYTALE_TASK_GROUP);
            task.setDescription("Runs the Hytale " + model.getName() + " configuration.");

            task.getMainClass().set(AGENT_MAIN_CLASS);
            task.classpath(agentConfiguration.getAgentJar());
            task.setWorkingDir(model.getWorkingDirectory());
            task.setJvmArgs(model.getJvmArguments().get());
            task.setArgs(model.getArguments().get());
            task.setEnabled(model.getEnabled().get());
            task.environment(
                    AGENT_CONFIGURATION_ENV_VARIABLE,
                    prepareRunTask.get().getAgentConfigurationFile().get().getAsFile().getAbsolutePath()
            );
            task.setStandardInput(System.in);
        });

        ideIntegration.runTaskOnSync(prepareRunTask);
        ideIntegration.addRunConfiguration(prepareRunTask, model, agentConfiguration);
    }

    private Provider<RegularFile> getAgentConfigurationFile(HytaleRunModel model) {
        return project.getLayout().getBuildDirectory().file(
                "hytale-dev/runs/" + model.getName() + "/agent-configuration.properties"
        );
    }
}
