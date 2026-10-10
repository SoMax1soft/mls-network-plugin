// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public enum ConsoleEcho {
    SILENT,
    ECHO_STAFF_LINES;

    public static ConsoleEcho fromSwitch(boolean alertsConsole) {
        return alertsConsole ? ECHO_STAFF_LINES : SILENT;
    }
}
