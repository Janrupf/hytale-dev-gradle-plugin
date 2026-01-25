package net.janrupf.gradle.hytale.dev.bridge;

import net.janrupf.gradle.hytale.dev.protocol.HytaleBridgeProto.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

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
                .build();

        AgentMessage message = AgentMessage.newBuilder()
                .setHello(hello)
                .build();

        send(message.toByteArray());
    }

    @Override
    public void onMessage(String message) {
        // Text messages not used - all communication is binary protobuf
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            byte[] data = new byte[bytes.remaining()];
            bytes.get(data);
            IdeMessage message = IdeMessage.parseFrom(data);
            handleIdeMessage(message);
        } catch (Exception e) {
            System.err.println("[DevBridge] Failed to parse IDE message: " + e.getMessage());
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
            default:
                // Unknown message type
                break;
        }
    }

    private void handleIdeHello(IdeHello hello) {
        System.out.println("[DevBridge] IDE connected: version " + hello.getPluginVersion());
    }

    private void handleGetCommands(GetCommandsRequest request) {
        // TODO: Implement command registry extraction in Phase 4
    }

    private void handleGetSuggestions(GetSuggestionsRequest request) {
        // TODO: Implement suggestion handling in Phase 4
    }

    private void handleExecuteCommand(ExecuteCommandRequest request) {
        // TODO: Implement command execution in Phase 4
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        connected = false;
        System.out.println("[DevBridge] Connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[DevBridge] Error: " + ex.getMessage());
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
