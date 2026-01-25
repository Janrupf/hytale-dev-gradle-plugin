package net.janrupf.gradle.hytale.dev.ide;

import net.janrupf.gradle.hytale.dev.agent.HytaleDevAgentConfiguration;
import net.janrupf.gradle.hytale.dev.extension.HytaleRunModel;
import net.janrupf.gradle.hytale.dev.tasks.PrepareHytaleServerRunTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.jspecify.annotations.NonNull;

/**
 * Integration with IDEs to improve the development experience.
 */
public interface IdeIntegration {
    /**
     * Runs the given task whenever the IDE syncs the project.
     *
     * @param task the task to run on sync
     */
    void runTaskOnSync(TaskProvider<? extends Task> task);

    /**
     * Called after all details have been configured and the plugin
     * is sure that no changes will be made anymore.
     */
    default void finalizeConfiguration() {
    }

    /**
     * Adds a run configuration for the given Hytale run model.
     *
     * @param prepareTask        the prepare task for the run configuration
     * @param model              the Hytale run model
     * @param agentConfiguration the development java agent configuration
     */
    default void addRunConfiguration(
            TaskProvider<PrepareHytaleServerRunTask> prepareTask,
            HytaleRunModel model,
            HytaleDevAgentConfiguration agentConfiguration
    ) {
    }

    /**
     * Auto-detects the IDE integration for the given project.
     *
     * @param project the gradle project
     * @return the detected IDE integration
     */
    static @NonNull IdeIntegration autoDetect(Project project) {
        if (Boolean.getBoolean("idea.active")) {
            return new IDEAIdeIntegration(project);
        } else if (System.getProperty("eclipse.application") != null) {
            return new EclipseIdeIntegration(project);
        }

        return NoOp.INSTANCE;
    }

    record NoOp() implements IdeIntegration {
        static final NoOp INSTANCE = new NoOp();

        @Override
        public void runTaskOnSync(TaskProvider<? extends Task> task) {
        }
    }
}
