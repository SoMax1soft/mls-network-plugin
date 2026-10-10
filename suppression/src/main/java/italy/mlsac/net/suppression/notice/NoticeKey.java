// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

public enum NoticeKey {
    ALERT_FLAG("suppression-alert-flag"),
    ALERT_BUFFER("suppression-alert-buffer");

    private final String key;

    NoticeKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
