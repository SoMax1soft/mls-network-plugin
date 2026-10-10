// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.suspect;

final class PresenceClock {

    private long offlineTotal;
    private long offlineSince;
    private boolean online = true;

    long playerTime(long now) {
        return online ? now - offlineTotal : offlineSince - offlineTotal;
    }

    long timeShift() {
        return offlineTotal;
    }

    boolean online() {
        return online;
    }

    void leave(long now) {
        if (online) {
            online = false;
            offlineSince = now;
        }
    }

    void rejoin(long now) {
        if (!online) {
            offlineTotal += Math.max(0L, now - offlineSince);
            online = true;
        }
    }

    long offlineFor(long now) {
        return online ? 0L : Math.max(0L, now - offlineSince);
    }
}
