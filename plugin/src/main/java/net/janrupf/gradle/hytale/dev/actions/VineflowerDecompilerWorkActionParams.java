package net.janrupf.gradle.hytale.dev.actions;

import org.gradle.api.Project;
import org.gradle.api.file.FileSystemLocationProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.workers.WorkParameters;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.util.HashMap;
import java.util.Map;

public interface VineflowerDecompilerWorkActionParams extends WorkParameters {
    Map<String, Object> DEFAULT_VINEFLOWER_PREFERENCES = Map.ofEntries(
            Map.entry(IFernflowerPreferences.DECOMPILE_INNER, true),
            Map.entry(IFernflowerPreferences.REMOVE_BRIDGE, true),
            Map.entry(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, true),
            Map.entry(IFernflowerPreferences.ASCII_STRING_CHARACTERS, true),
            Map.entry(IFernflowerPreferences.REMOVE_SYNTHETIC, true),
            Map.entry(IFernflowerPreferences.INCLUDE_ENTIRE_CLASSPATH, true),
            Map.entry(IFernflowerPreferences.IGNORE_INVALID_BYTECODE, true),
            Map.entry(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, true),
            Map.entry(IFernflowerPreferences.DUMP_CODE_LINES, true),
            Map.entry(IFernflowerPreferences.INDENT_STRING, "    ")
    );

    RegularFileProperty getInputJar();

    RegularFileProperty getDecompiledOutputJar();

    MapProperty<String, Object> getVineflowerPreferences();

    ListProperty<String> getPrefixes();

    static Provider<Map<String, Object>> defaultPreferencesProvider(Project project) {
        return project.provider(() -> {
            var mapCopy = new HashMap<>(DEFAULT_VINEFLOWER_PREFERENCES);

            if (!mapCopy.containsKey("log-level")) {
                Logger logger = project.getLogger();
                String logLevel;

                if (logger.isTraceEnabled()) {
                    logLevel = "trace";
                    // Vineflower has no debug level, trace is the most verbose, followed by info
                } else if (logger.isInfoEnabled()) {
                    logLevel = "info";
                } else if (logger.isWarnEnabled()) {
                    logLevel = "warn";
                } else {
                    logLevel = "error";
                }

                mapCopy.put("log-level", logLevel);
            }

            return mapCopy;
        });
    }
}
