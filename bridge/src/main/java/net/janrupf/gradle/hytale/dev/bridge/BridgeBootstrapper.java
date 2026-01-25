package net.janrupf.gradle.hytale.dev.bridge;

/**
 * Static initialization entry point called from bytecode injection.
 * <p>
 * This class is loaded reflectively by the agent after injecting a call
 * into HytaleLogger's static initializer. It provides early bridge
 * initialization before the main server code runs.
 */
public final class BridgeBootstrapper {
    private static volatile boolean initialized = false;
    private static DevBridgeClient client;
    private static LogSubscriber subscriber;

    private BridgeBootstrapper() {
        // Prevent instantiation
    }

    /**
     * Initialize the bridge if not already initialized.
     * <p>
     * This method is safe to call multiple times - subsequent calls will be no-ops.
     * If the required environment variables are not set, initialization is skipped silently.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Check for bridge environment variables
        String port = System.getenv("HYTALE_DEV_BRIDGE_PORT");
        String token = System.getenv("HYTALE_DEV_BRIDGE_TOKEN");

        if (port == null || token == null) {
            // Bridge not configured, skip initialization silently
            return;
        }

        try {
            int portNumber = Integer.parseInt(port);

            // Initialize bridge client and log subscriber
            client = new DevBridgeClient(portNumber, token);
            subscriber = new LogSubscriber(client);

            // Connect and subscribe (async)
            client.connect();
            subscriber.subscribe();

            System.out.println("[HytaleDev] Bridge initialized on port " + portNumber);
        } catch (NumberFormatException e) {
            System.err.println("[HytaleDev] Invalid bridge port number: " + port);
        } catch (Exception e) {
            System.err.println("[HytaleDev] Failed to initialize bridge: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shutdown the bridge gracefully.
     * <p>
     * This method can be called during server shutdown to clean up resources.
     */
    public static void shutdown() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                System.err.println("[HytaleDev] Failed to close bridge client: " + e.getMessage());
            }
        }
    }
}
