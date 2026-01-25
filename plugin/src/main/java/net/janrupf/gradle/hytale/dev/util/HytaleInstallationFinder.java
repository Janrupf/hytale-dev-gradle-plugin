package net.janrupf.gradle.hytale.dev.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility for finding the Hytale installation directory across different platforms.
 * <p>
 * This class provides methods to locate the Hytale game package directory,
 * server JAR, and assets ZIP file based on platform-specific installation paths.
 */
public final class HytaleInstallationFinder {
    private static final String HYTALE_LATEST_GAME_PACKAGE_DIR = "Hytale/install/release/package/game/latest";

    private HytaleInstallationFinder() {
        // Utility class
    }

    /**
     * Find the default Hytale game package directory.
     * <p>
     * Checks in order:
     * <ol>
     *   <li>System property: hytale.gamePackageDir</li>
     *   <li>Platform-specific locations (Windows/macOS/Linux)</li>
     *   <li>Flatpak fallback on Linux</li>
     * </ol>
     *
     * @return Path to game package directory, or null if not found
     */
    public static Path findGamePackageDir() {
        // 1. Check system property override
        String gamePackageOverride = System.getProperty("hytale.gamePackageDir");
        if (gamePackageOverride != null && !gamePackageOverride.isEmpty()) {
            Path overridePath = Paths.get(gamePackageOverride);
            if (Files.isDirectory(overridePath)) {
                return overridePath;
            }
        }

        String userHome = System.getProperty("user.home");
        Path dataHomePath = determineDataHomePath(userHome);

        // 2. Check standard platform location
        Path standardPath = dataHomePath.resolve(HYTALE_LATEST_GAME_PACKAGE_DIR);
        if (Files.isDirectory(standardPath)) {
            return standardPath;
        }

        // 3. Linux Flatpak fallback
        if (isUnixFamily()) {
            Path flatpakPath = Paths.get(userHome, ".var", "app",
                    "com.hypixel.HytaleLauncher", "data")
                    .resolve(HYTALE_LATEST_GAME_PACKAGE_DIR);
            if (Files.isDirectory(flatpakPath)) {
                return flatpakPath;
            }
        }

        return null;
    }

    /**
     * Find the HytaleServer.jar file.
     *
     * @return Path to HytaleServer.jar, or null if not found
     */
    public static Path findServerJar() {
        Path gamePackage = findGamePackageDir();
        if (gamePackage == null) {
            return null;
        }

        Path serverJar = gamePackage.resolve("Server").resolve("HytaleServer.jar");
        if (Files.isRegularFile(serverJar)) {
            return serverJar;
        }

        return null;
    }

    /**
     * Find the Assets.zip file.
     *
     * @return Path to Assets.zip, or null if not found
     */
    public static Path findAssetsZip() {
        Path gamePackage = findGamePackageDir();
        if (gamePackage == null) {
            return null;
        }

        Path assetsZip = gamePackage.resolve("Assets.zip");
        if (Files.isRegularFile(assetsZip)) {
            return assetsZip;
        }

        return null;
    }

    private static Path determineDataHomePath(String userHome) {
        if (isMacFamily()) {
            return Paths.get(userHome, "Library", "Application Support");
        } else if (isUnixFamily()) {
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            if (xdgDataHome != null && !xdgDataHome.isEmpty()) {
                return Paths.get(xdgDataHome);
            }
            return Paths.get(userHome, ".local", "share");
        } else if (isWindowsFamily()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isEmpty()) {
                return Paths.get(appData);
            }
            return Paths.get(userHome, "AppData", "Roaming");
        }
        // Fallback
        return Paths.get(userHome);
    }

    private static boolean isMacFamily() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    private static boolean isUnixFamily() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("aix");
    }

    private static boolean isWindowsFamily() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
}
