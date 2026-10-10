// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console;

import italy.mlsac.net.clock.Clock;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ConsoleLog implements PluginLog {

    private static final String DEBUG_MARKER = "[Debug] ";

    private final LogSink sink;
    private final SecretRedactor redactor;
    private final BooleanSupplier debugMode;
    private final RepeatSuppressor repeats;
    private final TraceThrottle traces;

    public ConsoleLog(LogSink sink, SecretRedactor redactor, BooleanSupplier debugMode, Clock clock) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
        this.debugMode = Objects.requireNonNull(debugMode, "debugMode");
        this.repeats = new RepeatSuppressor(clock);
        this.traces = new TraceThrottle(clock);
    }

    @Override
    public void info(LogTopic topic, String message) {
        sink.write(LogLevel.INFO, compose(topic, message), null);
    }

    @Override
    public void warn(LogTopic topic, String message) {
        folded(LogLevel.WARNING, topic, message, null);
    }

    @Override
    public void warn(LogTopic topic, String message, Throwable cause) {
        folded(LogLevel.WARNING, topic, message, traceFor(cause));
    }

    @Override
    public void error(LogTopic topic, String message) {
        folded(LogLevel.ERROR, topic, message, null);
    }

    @Override
    public void error(LogTopic topic, String message, Throwable cause) {
        folded(LogLevel.ERROR, topic, message, traceFor(cause));
    }

    @Override
    public void debug(LogTopic topic, Supplier<String> message) {
        if (debugMode.getAsBoolean()) {
            sink.write(LogLevel.DEBUG, DEBUG_MARKER + compose(topic, message.get()), null);
        }
    }

    @Override
    public boolean verboseOn() {
        return debugMode.getAsBoolean();
    }

    private Throwable traceFor(Throwable cause) {
        return cause != null && traces.allow(cause) ? new RedactedThrowable(cause, redactor) : null;
    }

    private void folded(LogLevel level, LogTopic topic, String message, Throwable trace) {
        repeats.admit(compose(topic, message)).ifPresent(text -> sink.write(level, text, trace));
    }

    private String compose(LogTopic topic, String message) {
        return redactor.redact(topic.tag() + message);
    }
}
