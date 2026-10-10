// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console;

public enum LogTopic {
    GENERAL(""),
    PLATFORM("Platform"),
    CONFIG("Settings"),
    BACKEND("Backend"),
    PRESET("Preset"),
    UPDATER("Updater"),
    DETECTION("Detection"),
    DATA("Data"),
    PENALTY("Penalty"),
    SUPPRESSION("Suppression"),
    NAMETAGS("Holograms"),
    PACKETS("Packets"),
    COMMANDS("Commands"),
    BENCH("Bench");

    private final String label;

    LogTopic(String label) {
        this.label = label;
    }

    public String tag() {
        return label.isEmpty() ? "" : '[' + label + "] ";
    }
}
