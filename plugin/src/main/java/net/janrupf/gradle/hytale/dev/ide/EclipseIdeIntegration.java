package net.janrupf.gradle.hytale.dev.ide;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

public class EclipseIdeIntegration implements IdeIntegration {
    private final EclipseModel eclipseModel;

    public EclipseIdeIntegration(Project project) {
        var eclipseModel = project.getExtensions().findByType(EclipseModel.class);
        if (eclipseModel == null) {
            project.getPluginManager().apply(EclipsePlugin.class);
            eclipseModel = project.getExtensions().findByType(EclipseModel.class);
        }

        this.eclipseModel = eclipseModel;
    }

    @Override
    public void runTaskOnSync(TaskProvider<? extends Task> task) {
        if (this.eclipseModel != null) {
            eclipseModel.synchronizationTasks(task);
        }
    }
}
