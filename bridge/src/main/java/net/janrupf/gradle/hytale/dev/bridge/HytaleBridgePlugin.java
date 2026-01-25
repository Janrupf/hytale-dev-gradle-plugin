package net.janrupf.gradle.hytale.dev.bridge;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Hytale plugin that provides a WebSocket bridge to the IntelliJ IDE.
 * <p>
 * This plugin is designed to be loaded via the agent's classpath injection,
 * enabling real-time communication between the running server and the IDE.
 * <p>
 * When the bridge environment variables are not present (e.g., when running
 * the server outside of IntelliJ), this plugin runs in standalone mode
 * without attempting to connect.
 */
public class HytaleBridgePlugin extends JavaPlugin {
    private DevBridgeClient bridgeClient;
    private LogSubscriber logSubscriber;

    public HytaleBridgePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("Hytale Dev Bridge initializing...");

        // Check for bridge connection environment variables
        String port = System.getenv("HYTALE_DEV_BRIDGE_PORT");
        String token = System.getenv("HYTALE_DEV_BRIDGE_TOKEN");

        if (port == null || token == null) {
            getLogger().at(Level.INFO).log("Bridge environment variables not set - running standalone");
            return;
        }

        try {
            int portNumber = Integer.parseInt(port);
            bridgeClient = new DevBridgeClient(portNumber, token);
            logSubscriber = new LogSubscriber(bridgeClient);
            getLogger().at(Level.INFO).log("Dev Bridge configured for port %d", portNumber);
        } catch (NumberFormatException e) {
            getLogger().at(Level.WARNING).log("Invalid bridge port: %s", port);
        }
    }

    @Override
    protected void start() {
        if (bridgeClient != null) {
            try {
                bridgeClient.connectBlocking();
                if (bridgeClient.isConnected()) {
                    logSubscriber.subscribe();
                    getLogger().at(Level.INFO).log("Dev Bridge connected to IDE");
                } else {
                    getLogger().at(Level.WARNING).log("Dev Bridge failed to connect to IDE");
                }
            } catch (InterruptedException e) {
                getLogger().at(Level.WARNING).withCause(e).log("Interrupted while connecting to IDE");
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected void shutdown() {
        if (logSubscriber != null) {
            logSubscriber.unsubscribe();
        }
        if (bridgeClient != null) {
            bridgeClient.disconnect();
            getLogger().at(Level.INFO).log("Dev Bridge disconnected");
        }
    }
}
