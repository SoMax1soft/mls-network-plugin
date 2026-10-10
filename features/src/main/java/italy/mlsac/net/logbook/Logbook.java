// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.logbook;

public interface Logbook {

    void info(String line);

    void warn(String line);

    void severe(String line, Throwable cause);

    void debug(String line);
}
