package net.janrupf.gradle.hytale.dev.agent;

import java.nio.file.Path;
import java.util.Objects;

public class BytecodeEntryPoints {
    @SuppressWarnings("unused") // called by transformed bytecode from transforms.AssetModuleTransformer
    public static Path redirectAssetPackPath(Path assetPackPath) {
        // During development the manifest.json may not be where the asset resources
        // are located. Redirect the asset pack path accordingly.
        var redirectSource = HytaleDevAgent.getAssetRedirectSource();
        var redirectTarget = HytaleDevAgent.getAssetRedirectTarget();

        if (Objects.equals(assetPackPath, redirectSource)) {
            return redirectTarget;
        }

        return assetPackPath;
    }

    @SuppressWarnings("unused") // called by transformed bytecode from transforms.BridgeInjectorTransformer
    public static void initializeBridge() {
        try {
            Class<?> bootstrapper = Thread.currentThread()
                    .getContextClassLoader()
                    .loadClass("net.janrupf.gradle.hytale.dev.bridge.BridgeBootstrapper");
            bootstrapper.getMethod("initialize").invoke(null);
        } catch (ClassNotFoundException e) {
            // Bridge not available, skip silently
        } catch (Exception e) {
            System.err.println("[HytaleDev] Failed to initialize bridge: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
