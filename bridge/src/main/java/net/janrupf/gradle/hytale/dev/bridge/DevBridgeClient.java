package net.janrupf.gradle.hytale.dev.bridge;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import net.janrupf.gradle.hytale.dev.protocol.HytaleBridgeProto.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * WebSocket client that connects to the IntelliJ IDE's Dev Bridge server.
 * <p>
 * Handles the bidirectional protocol communication between the running
 * Hytale server and the IDE for features like log forwarding, command
 * autocomplete, and asset path synchronization.
 */
public class DevBridgeClient extends WebSocketClient {
    private static final int PROTOCOL_VERSION = 1;
    private static final String AGENT_VERSION = "0.1.0";
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final String authToken;
    private volatile boolean connected = false;

    public DevBridgeClient(int port, String authToken) {
        super(URI.create("ws://localhost:" + port + "/hytale-dev-bridge"),
                createHeaders(authToken));
        this.authToken = authToken;
        this.setConnectionLostTimeout(30);
    }

    private static Map<String, String> createHeaders(String token) {
        return Collections.singletonMap("Authorization", "Bearer " + token);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        connected = true;

        // Send hello message
        AgentHello hello = AgentHello.newBuilder()
                .setProtocolVersion(PROTOCOL_VERSION)
                .setAgentVersion(AGENT_VERSION)
                .addCapabilities("logs")
                .addCapabilities("commands")
                .addCapabilities("assets")
                .addCapabilities("translate")
                .build();

        AgentMessage message = AgentMessage.newBuilder()
                .setHello(hello)
                .build();

        send(message.toByteArray());
    }

    @Override
    public void onMessage(String message) {
        LOGGER.at(Level.WARNING).log("Received unexpected text message: %s", message);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            byte[] data = new byte[bytes.remaining()];
            bytes.get(data);
            IdeMessage message = IdeMessage.parseFrom(data);
            handleIdeMessage(message);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to parse IDE message");
        }
    }

    private void handleIdeMessage(IdeMessage message) {
        switch (message.getPayloadCase()) {
            case HELLO:
                handleIdeHello(message.getHello());
                break;
            case GET_COMMANDS:
                handleGetCommands(message.getGetCommands());
                break;
            case GET_SUGGESTIONS:
                handleGetSuggestions(message.getGetSuggestions());
                break;
            case EXECUTE_COMMAND:
                handleExecuteCommand(message.getExecuteCommand());
                break;
            case TRANSLATE:
                handleTranslateRequest(message.getTranslate());
                break;
            default:
                LOGGER.at(Level.WARNING).log("Received unknown IDE message type: %s", message.getPayloadCase());
                break;
        }
    }

    private void handleIdeHello(IdeHello hello) {
        LOGGER.at(Level.INFO).log("IDE connected: version %s", hello.getPluginVersion());
    }

    private void handleGetCommands(GetCommandsRequest request) {
        LOGGER.at(Level.FINE).log("Extracting command registry...");
        CommandRegistryResponse response = CommandMetadataExtractor.getInstance().extractFullRegistry();
        AgentMessage message = AgentMessage.newBuilder()
                .setCommandRegistry(response)
                .build();
        send(message.toByteArray());
        LOGGER.at(Level.INFO).log("Sent %d commands to IDE", response.getCommandsCount());
    }

    private void handleGetSuggestions(GetSuggestionsRequest request) {
        SuggestionsResponse response = SuggestionHandler.getInstance().getSuggestions(request);
        AgentMessage message = AgentMessage.newBuilder()
                .setSuggestions(response)
                .build();
        send(message.toByteArray());
    }

    private void handleExecuteCommand(ExecuteCommandRequest request) {
        String command = request.getCommand();
        LOGGER.at(Level.INFO).log("Executing command from IDE: %s", command);

        CommandManager manager = CommandManager.get();
        if (manager != null) {
            manager.handleCommand(ConsoleSender.INSTANCE, command);
        }
    }

    private void handleTranslateRequest(TranslateRequest request) {
        String language = request.hasLanguage() ? request.getLanguage() : "en";
        I18nModule i18n = I18nModule.get();

        TranslateResponse.Builder builder = TranslateResponse.newBuilder();
        for (String key : request.getKeysList()) {
            String translated = i18n.getMessage(language, key);
            if (translated != null) {
                builder.putTranslations(key, translated);
            }
        }

        AgentMessage message = AgentMessage.newBuilder()
                .setTranslateResponse(builder.build())
                .build();
        send(message.toByteArray());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        connected = false;
        LOGGER.at(Level.INFO).log("Connection closed: %s", reason);
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.at(Level.WARNING).withCause(ex).log("Connection error");
    }

    /**
     * Send a log event to the IDE.
     *
     * @param logEvent the log event to send
     */
    public void sendLogEvent(LogEvent logEvent) {
        if (!connected) return;

        AgentMessage message = AgentMessage.newBuilder()
                .setLogEvent(logEvent)
                .build();

        send(message.toByteArray());
    }

    /**
     * Send a server state event to the IDE.
     *
     * @param state the server state
     */
    public void sendServerState(ServerState state) {
        if (!connected) return;

        ServerStateEvent event = ServerStateEvent.newBuilder()
                .setState(state)
                .build();

        AgentMessage message = AgentMessage.newBuilder()
                .setServerState(event)
                .build();

        send(message.toByteArray());
    }

    /**
     * Send asset paths to the IDE.
     * <p>
     * These paths point to plugin resource directories that the IDE
     * should include in its asset scanning.
     *
     * @param paths list of absolute paths to plugin asset directories
     */
    public void sendAssetPaths(List<String> paths) {
        if (!connected) return;

        AssetPathsEvent event = AssetPathsEvent.newBuilder()
                .addAllPaths(paths)
                .build();

        AgentMessage message = AgentMessage.newBuilder()
                .setAssetPaths(event)
                .build();

        send(message.toByteArray());
        LOGGER.at(Level.INFO).log("Sent %d asset paths to IDE", paths.size());
    }

    /**
     * Check if the client is currently connected to the IDE.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connected && isOpen();
    }

    /**
     * Disconnect from the IDE gracefully.
     */
    public void disconnect() {
        if (isOpen()) {
            close();
        }
    }
}
