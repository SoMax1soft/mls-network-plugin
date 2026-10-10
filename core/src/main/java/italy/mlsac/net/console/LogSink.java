// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console;

public interface LogSink {

    void write(LogLevel level, String line, Throwable attachedTrace);
}
