package net.janrupf.gradle.hytale.dev.extension;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The manifest of a Hytale plugin.
 */
public abstract class HytaleManifestModel {
    /**
     * The identifier of the Hytale plugin.
     *
     * @return the identifier property
     */
    public abstract Property<Identifier> getIdentifier();

    /**
     * The version of the Hytale plugin.
     *
     * @return the version property
     */
    public abstract Property<String> getVersion();

    /**
     * The description of the Hytale plugin.
     *
     * @return the description property
     */
    public abstract Property<String> getDescription();

    /**
     * The authors of the Hytale plugin.
     *
     * @return the list of authors
     */
    public abstract ListProperty<Author> getAuthors();

    /**
     * The website of the Hytale plugin.
     *
     * @return the website property
     */
    public abstract Property<String> getWebsite();

    /**
     * The main class of the Hytale plugin.
     *
     * @return the main class property
     */
    public abstract Property<String> getMain();

    /**
     * The version of the Hytale server this plugin is built for.
     *
     * @return the server version property
     */
    public abstract Property<String> getServerVersion();

    /**
     * The dependencies of the Hytale plugin.
     * <p>
     * Maps from plugin identifier to version requirement.
     *
     * @return the map of dependencies
     */
    public abstract MapProperty<Identifier, String> getDependencies();

    /**
     * The optional dependencies of the Hytale plugin.
     * <p>
     * Maps from plugin identifier to version requirement.
     *
     * @return the map of optional dependencies
     */
    public abstract MapProperty<Identifier, String> getOptionalDependencies();

    /**
     * Plugins which this plugin should be loaded before.
     *
     * @return the map of plugins which should load after this plugin
     */
    public abstract MapProperty<Identifier, String> getLoadBefore();

    /**
     * Whether this plugin is disabled by default.
     *
     * @return the is disabled by default property
     */
    public abstract Property<Boolean> getDisabledByDefault();

    /**
     * Whether this plugin includes an asset pack.
     *
     * @return the includes asset pack property
     */
    public abstract Property<Boolean> getIncludesAssetPack();

    private final Project project;

    @Inject
    public HytaleManifestModel(Project project) {
        this.project = project;

        var defaultIdentifier = project.getObjects().newInstance(Identifier.class);
        defaultIdentifier.getGroup().set(project.provider(() -> project.getGroup().toString()));
        defaultIdentifier.getName().set(project.provider(project::getName));

        getIdentifier().convention(defaultIdentifier);
        getVersion().convention(project.provider(() -> project.getVersion().toString()));
        getDescription().convention(project.provider(project::getDescription));

        getAuthors().convention(Collections.emptyList());
        getServerVersion().convention("*");
        getDependencies().convention(Collections.emptyMap());
        getOptionalDependencies().convention(Collections.emptyMap());
        getLoadBefore().convention(Collections.emptyMap());
        getDisabledByDefault().convention(false);
        getIncludesAssetPack().convention(true);
    }

    /**
     * Change the identifier of the Hytale plugin.
     *
     * @param name the name
     * @param group the group
     */
    public void identifier(String name, String group) {
        getIdentifier().get().getName().set(name);
        getIdentifier().get().getGroup().set(group);
    }

    /**
     * Change the name of the Hytale plugin.
     *
     * @param name the name
     */
    public void name(String name) {
        getIdentifier().get().getName().set(name);
    }

    /**
     * Change the group of the Hytale plugin.
     *
     * @param group the group
     */
    public void group(String group) {
        getIdentifier().get().getGroup().set(group);
    }

    /**
     * Change the version of the Hytale plugin.
     *
     * @param version the version
     */
    public void version(String version) {
        getVersion().set(version);
    }

    /**
     * Change the description of the Hytale plugin.
     *
     * @param description the description
     */
    public void description(String description) {
        getDescription().set(description);
    }

    /**
     * Add an author to the Hytale plugin.
     *
     * @param action the action to configure the author
     */
    public void author(Action<? super Author> action) {
        var author = project.getObjects().newInstance(Author.class);
        action.execute(author);
        getAuthors().add(author);
    }

    /**
     * Change the website of the Hytale plugin.
     *
     * @param website the website
     */
    public void website(String website) {
        getWebsite().set(website);
    }

    /**
     * Change the main class of the Hytale plugin.
     *
     * @param mainClass the main class
     */
    @SuppressWarnings("ConfusingMainMethod") // Should be clear in this context...
    public void main(String mainClass) {
        getMain().set(mainClass);
    }

    /**
     * Change the server version this plugin is built for.
     *
     * @param version the server version
     */
    public void serverVersion(String version) {
        getServerVersion().set(version);
    }

    /**
     * Add a dependency to the Hytale plugin.
     *
     * @param id the identifier of the dependency
     * @param versionRange the version requirement of the dependency
     */
    public void dependency(Identifier id, String versionRange) {
        getDependencies().put(id, versionRange);
    }

    /**
     * Add a dependency to the Hytale plugin.
     *
     * @param group the group of the dependency
     * @param name the name of the dependency
     * @param versionRange the version requirement of the dependency
     */
    public void dependency(String group, String name, String versionRange) {
        var identifier = project.getObjects().newInstance(Identifier.class);
        identifier.getGroup().set(group);
        identifier.getName().set(name);

        dependency(identifier, versionRange);
    }

    /**
     * Add an optional dependency to the Hytale plugin.
     *
     * @param id the identifier of the optional dependency
     * @param versionRange the version requirement of the optional dependency
     */
    public void optionalDependency(Identifier id, String versionRange) {
        getOptionalDependencies().put(id, versionRange);
    }

    /**
     * Add an optional dependency to the Hytale plugin.
     *
     * @param group the group of the optional dependency
     * @param name the name of the optional dependency
     * @param versionRange the version requirement of the optional dependency
     */
    public void optionalDependency(String group, String name, String versionRange) {
        var identifier = project.getObjects().newInstance(Identifier.class);
        identifier.getGroup().set(group);
        identifier.getName().set(name);

        optionalDependency(identifier, versionRange);
    }

    /**
     * Specify that this plugin should be loaded before another plugin.
     *
     * @param id the identifier of the other plugin
     * @param versionRange the version requirement of the other plugin
     */
    public void loadBefore(Identifier id, String versionRange) {
        getLoadBefore().put(id, versionRange);
    }

    /**
     * Specify that this plugin should be loaded before another plugin.
     *
     * @param group the group of the other plugin
     * @param name the name of the other plugin
     * @param versionRange the version requirement of the other plugin
     */
    public void loadBefore(String group, String name, String versionRange) {
        var identifier = project.getObjects().newInstance(Identifier.class);
        identifier.getGroup().set(group);
        identifier.getName().set(name);

        loadBefore(identifier, versionRange);
    }

    /**
     * Set whether this plugin is disabled by default.
     *
     * @param disabled whether the plugin is disabled by default
     */
    public void disableByDefault(boolean disabled) {
        getDisabledByDefault().set(disabled);
    }

    /**
     * Set whether this plugin includes an asset pack.
     *
     * @param includes whether the plugin includes an asset pack
     */
    public void includesAssetPack(boolean includes) {
        getIncludesAssetPack().set(includes);
    }

    /**
     * A single author of a Hytale plugin.
     */
    public static abstract class Author {
        private final Project project;

        @Inject
        public Author(Project project) {
            this.project = project;
        }

        /**
         * The name of the author.
         *
         * @return the name property
         */
        public abstract Property<String> getName();

        /**
         * The email of the author.
         *
         * @return the email property
         */
        public abstract Property<String> getEmail();

        /**
         * The URL (homepage) of the author.
         *
         * @return the URL property
         */
        public abstract Property<String> getUrl();

        /**
         * Set the name of the author.
         *
         * @param name the name
         */
        public void name(String name) {
            getName().set(name);
        }

        /**
         * Set the email of the author.
         *
         * @param email the email
         */
        public void email(String email) {
            getEmail().set(email);
        }

        /**
         * Set the URL (homepage) of the author.
         *
         * @param url the URL
         */
        public void url(String url) {
            getUrl().set(url);
        }

        /**
         * Set the URL (homepage) of the author.
         *
         * @param uri the URI
         */
        public void url(URI uri) {
            getUrl().set(uri.toString());
        }

        /**
         * Set the URL (homepage) of the author.
         *
         * @param url the URL
         */
        public void url(URL url) {
            getUrl().set(url.toString());
        }

        public Provider<Map<String, Object>> toMap() {
            return project.provider(() -> {
                var out = new HashMap<String, Object>();

                // It doesn't make sense for them to be null, but it is valid
                // as far as I can tell from the Hytale decompilation...
                if (getName().isPresent()) {
                    out.put("Name", getName().get());
                }

                if (getEmail().isPresent()) {
                    out.put("Email", getEmail().get());
                }

                if (getUrl().isPresent()) {
                    out.put("Url", getUrl().get());
                }

                return out;
            });
        }
    }

    /**
     * An identifier for a Hytale plugin.
     */
    public static abstract class Identifier {
        /**
         * The group of the identifier.
         *
         * @return the group property
         */
        public abstract Property<String> getGroup();

        /**
         * The name of the identifier.
         *
         * @return the name property
         */
        public abstract Property<String> getName();

        /**
         * Convert the identifier to a manifest string.
         *
         * @return the manifest string provider
         */
        public Provider<String> toManifestString() {
            return getGroup().zip(getName(), (g, n) -> g + ":" + n);
        }
    }

    /**
     * Serialize the manifest to a map.
     * <p>
     * This generates a structure as expected by Hytale for plugin manifests.
     *
     * @return the manifest as a map
     */
    public Provider<Map<String, Object>> toMap() {
        return project.provider(() -> {
            var out = new HashMap<String, Object>();

            out.put("Group", getIdentifier().get().getGroup().get());
            out.put("Name", getIdentifier().get().getName().get());
            out.put("Version", getVersion().get());

            if (getDescription().isPresent()) {
                out.put("Description", getDescription().get());
            }

            var authors = new ArrayList<Map<String, Object>>();
            for (var author : getAuthors().get()) {
                authors.add(author.toMap().get());
            }

            out.put("Authors", authors);

            if (getWebsite().isPresent()) {
                out.put("Website", getWebsite().get());
            }

            // No idea why its nullable, but it is inside Hytale...
            if (getMain().isPresent()) {
                out.put("Main", getMain().get());
            }

            out.put("ServerVersion", getServerVersion().get());

            var dependencies = new HashMap<String, String>();
            for (var entry : getDependencies().get().entrySet()) {
                dependencies.put(entry.getKey().toManifestString().get(), entry.getValue());
            }

            out.put("Dependencies", dependencies);

            var optionalDependencies = new HashMap<String, String>();
            for (var entry : getOptionalDependencies().get().entrySet()) {
                optionalDependencies.put(entry.getKey().toManifestString().get(), entry.getValue());
            }
            out.put("OptionalDependencies", optionalDependencies);

            var loadBefore = new HashMap<String, String>();
            for (var entry : getLoadBefore().get().entrySet()) {
                loadBefore.put(entry.getKey().toManifestString().get(), entry.getValue());
            }
            out.put("LoadBefore", loadBefore);

            out.put("DisabledByDefault", getDisabledByDefault().get());
            out.put("IncludesAssetPack", getIncludesAssetPack().get());

            return out;
        });
    }
}
