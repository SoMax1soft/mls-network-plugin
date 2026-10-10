// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console.jul;

import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.console.LogSink;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class JulLogSink implements LogSink {

    private final Logger logger;

    public JulLogSink(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void write(LogLevel level, String line, Throwable attachedTrace) {
        Level julLevel = switch (level) {
            case DEBUG, INFO -> Level.INFO;
            case WARNING -> Level.WARNING;
            case ERROR -> Level.SEVERE;
        };
        if (attachedTrace == null) {
            logger.log(julLevel, line);
        } else {
            logger.log(julLevel, line, attachedTrace);
        }
    }
}
