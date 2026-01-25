package net.janrupf.gradle.hytale.dev.bridge;

import com.hypixel.hytale.logger.backend.HytaleLoggerBackend;
import net.janrupf.gradle.hytale.dev.protocol.HytaleBridgeProto.LogEvent;
import net.janrupf.gradle.hytale.dev.protocol.HytaleBridgeProto.LogLevel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Subscribes to the Hytale logger backend and forwards logs to the IDE.
 * <p>
 * <b>ODDITY:</b> The server's {@link HytaleLoggerBackend#subscribe(CopyOnWriteArrayList)}
 * expects a CopyOnWriteArrayList and calls {@code add()} on subscribers (lines 98-100
 * in HytaleLoggerBackend.java). We subclass and override {@code add()} to get
 * callback-style notification instead of storing log records.
 */
public class LogSubscriber {
    private final DevBridgeClient client;
    private final LogRecordCallback callback;

    public LogSubscriber(DevBridgeClient client) {
        this.client = client;
        this.callback = new LogRecordCallback(this::onLogRecord);
    }

    /**
     * Subscribe to the Hytale logger backend to receive log events.
     */
    public void subscribe() {
        HytaleLoggerBackend.subscribe(callback);
    }

    /**
     * Unsubscribe from the Hytale logger backend.
     */
    public void unsubscribe() {
        HytaleLoggerBackend.unsubscribe(callback);
    }

    private void onLogRecord(LogRecord record) {
        if (!client.isConnected()) {
            return;
        }

        LogEvent.Builder builder = LogEvent.newBuilder()
                .setTimestamp(record.getMillis())
                .setLevel(convertLevel(record.getLevel()))
                .setLoggerName(record.getLoggerName() != null ? record.getLoggerName() : "")
                .setMessage(record.getMessage() != null ? record.getMessage() : "");

        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(sw));
            builder.setThrowable(sw.toString());
        }

        builder.setThreadName(Thread.currentThread().getName());

        client.sendLogEvent(builder.build());
    }

    private LogLevel convertLevel(Level level) {
        if (level == null) {
            return LogLevel.LOG_LEVEL_UNKNOWN;
        }

        int value = level.intValue();
        if (value >= Level.SEVERE.intValue()) {
            return LogLevel.LOG_LEVEL_ERROR;
        } else if (value >= Level.WARNING.intValue()) {
            return LogLevel.LOG_LEVEL_WARNING;
        } else if (value >= Level.INFO.intValue()) {
            return LogLevel.LOG_LEVEL_INFO;
        } else if (value >= Level.FINE.intValue()) {
            return LogLevel.LOG_LEVEL_DEBUG;
        } else {
            return LogLevel.LOG_LEVEL_TRACE;
        }
    }

    /**
     * Subclass hack to intercept log records.
     * <p>
     * The HytaleLoggerBackend subscription API expects a CopyOnWriteArrayList
     * and adds log records to it. We override {@code add()} to get immediate
     * callbacks instead of storing records.
     */
    private static class LogRecordCallback extends CopyOnWriteArrayList<LogRecord> {
        private final Consumer<LogRecord> onLog;

        LogRecordCallback(Consumer<LogRecord> onLog) {
            this.onLog = onLog;
        }

        @Override
        public boolean add(LogRecord record) {
            // Immediate callback when server logs
            try {
                onLog.accept(record);
            } catch (Exception e) {
                // Don't let callback errors break logging
                System.err.println("[DevBridge] Error in log callback: " + e.getMessage());
            }
            // Don't store - we've already handled it
            return false;
        }
    }
}
