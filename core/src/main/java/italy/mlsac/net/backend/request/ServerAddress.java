// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

public record ServerAddress(String ip, int port) {

    public static final String UNKNOWN_IP = "unknown";

    public String reportedIp() {
        String trimmed = ip == null ? "" : ip.trim();
        return trimmed.isEmpty() ? UNKNOWN_IP : trimmed;
    }
}
