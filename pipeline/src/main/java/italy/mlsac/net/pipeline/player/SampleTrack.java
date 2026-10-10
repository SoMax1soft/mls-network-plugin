// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

final class SampleTrack {
    private static final long PING_UNKNOWN = Long.MIN_VALUE;

    private final PositionHistory history = new PositionHistory();
    private volatile PlayerSample sample;
    private volatile long ping = PING_UNKNOWN;

    PlayerSample replaceSample(PlayerSample next) {
        PlayerSample previous = sample;
        sample = next;
        history.append(next);
        return previous;
    }

    PlayerSample sample() {
        return sample;
    }

    void dropSample() {
        sample = null;
    }

    void dropSampleAndHistory() {
        sample = null;
        history.clear();
    }

    PositionHistory history() {
        return history;
    }

    void storePing(int millis) {
        ping = millis;
    }

    int pingOr(int fallback) {
        long known = ping;
        return known == PING_UNKNOWN ? fallback : (int) known;
    }
}
