package net.janrupf.gradle.hytale.dev.extension;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

/**
 * The Hytale extension for Gradle projects.
 * <p>
 * Configures Hytale-specific settings and properties.
 */
public abstract class HytaleExtension {
    private static final String HYTALE_LATEST_GAME_PACKAGE_DIR = "Hytale/install/release/package/game/latest";

    /**
     * The Hytale server JAR file.
     *
     * @return the server JAR file property
     */
    public abstract RegularFileProperty getServerJar();

    /**
     * The Hytale assets ZIP file.
     *
     * @return the assets ZIP file property
     */
    public abstract RegularFileProperty getAssetsZip();

    /**
     * Whether to enable decompiling the server JAR.
     *
     * @return the enable decompile server JAR property
     */
    public abstract Property<Boolean> getEnableDecompileServerJar();

    /**
     * The Hytale manifest model.
     *
     * @return the manifest property
     */
    public abstract Property<HytaleManifestModel> getManifest();

    /**
     * The list of manifest modifiers to apply when generating a Hytale manifest.
     *
     * @return the list of manifest modifiers
     */
    public abstract ListProperty<Action<? super Map<String, Object>>> getManifestModifiers();

    private final Project project;
    private final NamedDomainObjectContainer<HytaleRunModel> runs;

    @Inject
    public HytaleExtension(Project project) {
        this.project = project;
        this.runs = project.getObjects().domainObjectContainer(
                HytaleRunModel.class, (name) -> project.getObjects().newInstance(
                        HytaleRunModel.class,
                        name,
                        project,
                        getServerJar(),
                        getAssetsZip()
                )
        );

        this.runs.create("server");

        configureDefaults();
    }

    private void configureDefaults() {
        var defaultLatestGamePackageProvider = findDefaultLatestGamePackageDir();

        getServerJar().convention(defaultLatestGamePackageProvider.map(
                (packageDir) -> packageDir.dir("Server").file("HytaleServer.jar")));
        getAssetsZip().convention(defaultLatestGamePackageProvider.map(
                (packageDir) -> packageDir.file("Assets.zip")));
        getEnableDecompileServerJar().convention(true);
        getManifestModifiers().convention(Collections.emptyList());
        getManifest().convention(project.getObjects().newInstance(HytaleManifestModel.class, project));
    }

    /**
     * Configure the available run configurations.
     *
     * @param action the action to configure the run configurations
     */
    public void runs(Action<NamedDomainObjectContainer<HytaleRunModel>> action) {
        action.execute(runs);
    }

    /**
     * Configure the plugin manifest.
     *
     * @param action the action to configure the manifest
     */
    public void manifest(Action<? super HytaleManifestModel> action) {
        action.execute(getManifest().get());
    }

    /**
     * Adds a manifest modifier action to be applied when generating a Hytale manifest.
     *
     * @param action the manifest modifier action
     */
    public void modifyManifest(Action<? super Map<String, Object>> action) {
        getManifestModifiers().add(action);
    }

    private Provider<Directory> findDefaultLatestGamePackageDir() {
        return project.provider(() -> {
            // Check for overrides
            var gamePackageDir = project.getProperties().get("hytale.gamePackageDir");
            if (gamePackageDir != null) {
                return project.getLayout().getProjectDirectory().dir(gamePackageDir.toString());
            }

            var userHome = System.getProperty("user.home");

            Path dataHomePath;

            if (Os.isFamily(Os.FAMILY_MAC)) {
                dataHomePath = Paths.get(userHome, "Application Support");
            } else if (Os.isFamily(Os.FAMILY_UNIX)) {
                var dataHome = System.getenv("XDG_DATA_HOME");
                if (dataHome == null || dataHome.isEmpty()) {
                    dataHomePath = Paths.get(userHome, ".local", "share");
                } else {
                    dataHomePath = Paths.get(dataHome);
                }
            } else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                var appData = System.getenv("APPDATA");
                if (appData != null && !appData.isEmpty()) {
                    dataHomePath = Paths.get(appData);
                } else {
                    dataHomePath = Paths.get(userHome, "AppData");
                }
            } else {
                // Fallback to user home
                dataHomePath = Paths.get(userHome);
            }

            var file = dataHomePath.resolve(HYTALE_LATEST_GAME_PACKAGE_DIR).toFile();

            if (file.isDirectory()) {
                return project.getLayout().getProjectDirectory().dir(file.getAbsolutePath());
            }

            if (Os.isFamily(Os.FAMILY_UNIX)) {
                // Flatpack fallback
                var flatpakDataHome = Paths.get(userHome, ".var", "app", "com.hypixel.HytaleLauncher", "data");

                file = flatpakDataHome.resolve(HYTALE_LATEST_GAME_PACKAGE_DIR).toFile();
                if (file.isDirectory()) {
                    return project.getLayout().getProjectDirectory().dir(file.getAbsolutePath());
                }
            }

            return null;
        });
    }

    public NamedDomainObjectContainer<HytaleRunModel> getRuns() {
        return runs;
    }
}
