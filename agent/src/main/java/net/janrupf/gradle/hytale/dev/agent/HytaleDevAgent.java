package net.janrupf.gradle.hytale.dev.agent;

import net.janrupf.gradle.hytale.dev.agent.loader.HytaleDevAgentClassloader;
import net.janrupf.gradle.hytale.dev.agent.transforms.AssetModuleTransformer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

public class HytaleDevAgent {
    // Why does this exist?
    //
    // Well, turns out, IDE's struggle with dependencies that are not part of a gradle source
    // set. Gradle itself can deal with this just fine (ie. a JavaExec task can have additional
    // dependencies added to its classpath that are not part of the project source sets).
    // However, IDE's like IntelliJ IDEA and Eclipse do not understand this, and then fail
    // to run the application properly.
    //
    // Additionally IntelliJ likes to route running through Gradle, which causes some functionality
    // to ceise to work. In order to force IDE's to run the application directly, we generate configurations
    // that launch the agent jar, which then performs the classpath setup and launches the actual main class.

    private static Path assetRedirectSource;
    private static Path assetRedirectTarget;

    public static void main(String[] args) throws Throwable /* Transparent pass through for wrapped exceptions */ {
        var configurationFile = System.getenv("HYTALE_DEV_AGENT_CONFIGURATION");
        if (configurationFile == null) {
            throw new RuntimeException("HYTALE_DEV_AGENT_CONFIGURATION environment variable is not set");
        }

        var properties = new Properties();

        try (var reader = Files.newBufferedReader(Paths.get(configurationFile))) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read agent configuration file", e);
        }

        var urls = loadClassPath(properties.getProperty("classpath"));
        var mainClassName = properties.getProperty("mainClassName");

        if (properties.containsKey("asset.redirect.source") && properties.containsKey("asset.redirect.target")) {
            assetRedirectSource = Paths.get(properties.getProperty("asset.redirect.source"));
            assetRedirectTarget = Paths.get(properties.getProperty("asset.redirect.target"));
        }

        var delegatingClassLoader = new HytaleDevAgentClassloader(
                "Hytale",
                urls,
                Thread.currentThread().getContextClassLoader()
        );
        delegatingClassLoader.addTransformer(new AssetModuleTransformer());

        try {
            Thread.currentThread().setContextClassLoader(delegatingClassLoader);

            var mainClass = delegatingClassLoader.loadClass(mainClassName);

            mainClass.getMethod("main", String[].class)
                    .invoke(null, (Object) args);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load main class: " + mainClassName, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Main class does not have a main method: " + mainClassName, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access main method of class: " + mainClassName, e);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private static URL[] loadClassPath(String encodedClassPath) {
        if (encodedClassPath == null) {
            return new URL[0];
        }

        var base64Decoder = Base64.getDecoder();
        var entries = encodedClassPath.split(",");
        var urls = new URL[entries.length];

        for (int i = 0; i < entries.length; i++) {
            var decodedPath = new String(base64Decoder.decode(entries[i]), StandardCharsets.UTF_8);

            try {
                urls[i] = URI.create(decodedPath).toURL();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load classpath entry: " + decodedPath, e);
            }
        }

        return urls;
    }

    public static Path getAssetRedirectSource() {
        return assetRedirectSource;
    }

    public static Path getAssetRedirectTarget() {
        return assetRedirectTarget;
    }
}
