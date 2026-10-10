// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.suspect;

public record RetentionPolicy(long offlineRetentionMillis) {

    private static final long FIFTEEN_MINUTES = 15L * 60L * 1000L;

    public static RetentionPolicy standard() {
        return new RetentionPolicy(FIFTEEN_MINUTES);
    }

    boolean expired(long offlineFor) {
        return offlineFor > offlineRetentionMillis;
    }
}
