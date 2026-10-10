// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console;

import java.util.function.Supplier;

public interface PluginLog {

    void info(LogTopic topic, String message);

    void warn(LogTopic topic, String message);

    void warn(LogTopic topic, String message, Throwable cause);

    void error(LogTopic topic, String message);

    void error(LogTopic topic, String message, Throwable cause);

    void debug(LogTopic topic, Supplier<String> message);

    boolean verboseOn();
}
