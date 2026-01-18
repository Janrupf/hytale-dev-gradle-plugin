package net.janrupf.gradle.hytale.dev.extension;

import net.janrupf.gradle.hytale.dev.util.NamingUtil;
import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.Dependencies;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public abstract class HytaleRunModel implements Named, Dependencies {
    private static final String ALLOWED_NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";
    private static final String DEFAULT_MAIN_CLASS_NAME = "com.hypixel.hytale.Main";

    private final String name;

    /**
     * The directory this run configuration will use as working directory.
     *
     * @return the working directory property
     */
    public abstract DirectoryProperty getWorkingDirectory();

    /**
     * The arguments to pass to the Hytale server on launch.
     *
     * @return the list of arguments
     */
    public abstract ListProperty<String> getArguments();

    /**
     * The JVM arguments to pass to the Hytale server on launch.
     *
     * @return the list of JVM arguments
     */
    public abstract ListProperty<String> getJvmArguments();

    /**
     * The environment variables to set for the Hytale server process.
     *
     * @return the map of environment variables
     */
    public abstract MapProperty<String, String> getEnvironment();

    /**
     * Whether to allow operator commands in the server.
     *
     * @return the allow op property
     */
    public abstract Property<Boolean> getAllowOp();

    /**
     * The assets zip file to use for this run configuration.
     *
     * @return the assets zip file property
     */
    public abstract RegularFileProperty getAssetsZip();

    /**
     * The server jar file to use for this run configuration.
     *
     * @return the server jar file property
     */
    public abstract RegularFileProperty getServerJar();

    /**
     * The source set this run configuration is associated with.
     *
     * @return the source set property
     */
    public abstract Property<SourceSet> getSourceSet();

    /**
     * The main class name to launch.
     *
     * @return the main class name property
     */
    public abstract Property<String> getMainClassName();

    /**
     * Whether this run configuration is enabled.
     *
     * @return the enabled property
     */
    public abstract Property<Boolean> getEnabled();

    /**
     * The IDE name for this run configuration.
     *
     * @return the IDE name property
     */
    public abstract Property<String> getIdeName();

    /**
     * The location of the plugin assets.
     *
     * @return the assets location property
     */
    public abstract Property<FileSystemLocation> getAssetsLocation();

    @Inject
    public HytaleRunModel(
            String name,
            Project project,
            RegularFileProperty serverJar,
            RegularFileProperty assetsZip
    ) {
        this.name = name;
        checkName(name);

        getWorkingDirectory().convention(project.getLayout().getProjectDirectory().dir("run").dir(name));
        getArguments().convention(project.provider(this::buildConventionArguments));
        getAllowOp().convention(true);
        getServerJar().convention(serverJar);
        getAssetsZip().convention(assetsZip);
        getSourceSet().convention(project.provider(this::findDefaultSourceSet));
        getMainClassName().convention(DEFAULT_MAIN_CLASS_NAME);
        getEnabled().convention(true);
        getIdeName().convention(NamingUtil.capitalizeFirstLetter(name));
        getAssetsLocation().convention(findAssetsLocation());
    }

    /**
     * Add an argument to the run configuration.
     *
     * @param argument the argument to add
     */
    public void arg(String argument) {
        getArguments().add(argument);
    }

    /**
     * Add a JVM argument to the run configuration.
     *
     * @param jvmArgument the JVM argument to add
     */
    public void jvmArg(String jvmArgument) {
        getJvmArguments().add(jvmArgument);
    }

    /**
     * Add an environment variable to the run configuration.
     *
     * @param key   the environment variable key
     * @param value the environment variable value
     */
    public void environment(String key, String value) {
        getEnvironment().put(key, value);
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    private List<String> buildConventionArguments() {
        var out = new ArrayList<String>();

        if (getAllowOp().get()) {
            out.add("--allow-op");
        }

        out.add("--disable-sentry");
        out.add("--assets=" + getAssetsZip().getAsFile().get().getAbsolutePath());

        return out;
    }

    private SourceSet findDefaultSourceSet() {
        var sourceSetContainer = getProject().getExtensions().findByType(SourceSetContainer.class);

        if (sourceSetContainer == null) {
            return null;
        }

        return sourceSetContainer.findByName(SourceSet.MAIN_SOURCE_SET_NAME);
    }

    private Provider<FileSystemLocation> findAssetsLocation() {
        return getSourceSet()
                .flatMap(
                        (sourceSet) -> sourceSet.getResources()
                                .getSourceDirectories()
                                .getElements()
                ).map((dirs) -> {
                    if (dirs.size() != 1) {
                        throw new IllegalStateException(
                                "Expected exactly one resource directory for Hytale assets, manually set hytale.assetsLocation property."
                        );
                    }

                    return dirs.iterator().next();
                });
    }

    private static void checkName(String name) {
        for (char c : name.toCharArray()) {
            if (ALLOWED_NAME_CHARS.indexOf(c) == -1) {
                throw new IllegalArgumentException("Invalid run configuration name: " + name + ". Allowed characters A-Z, a-z, 0-9, - and _");
            }
        }
    }
}
