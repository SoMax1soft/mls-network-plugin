// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.integrity;

import italy.mlsac.net.config.integrity.BadPacketSettings;
import italy.mlsac.net.detection.BadPacketKind;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BadPacketTally {

    static final int REMEMBERED_PER_KIND = 1024;

    private final Map<UUID, Map<BadPacketKind, Track>> tracks = new ConcurrentHashMap<>();

    public TallyReading observe(UUID player, BadPacketKind kind, long nowMillis, BadPacketSettings settings) {
        Map<BadPacketKind, Track> kinds = tracks.computeIfAbsent(player, ignored -> new EnumMap<>(BadPacketKind.class));
        Track track;
        synchronized (kinds) {
            track = kinds.computeIfAbsent(kind, ignored -> new Track());
        }
        return track.observe(nowMillis, settings);
    }

    public void forget(UUID player) {
        tracks.remove(player);
    }

    public void clear() {
        tracks.clear();
    }

    private static final class Track {
        private final ArrayDeque<Long> seenAt = new ArrayDeque<>();
        private boolean struck;
        private long struckAt;

        synchronized TallyReading observe(long nowMillis, BadPacketSettings settings) {
            while (!seenAt.isEmpty() && nowMillis - seenAt.peekFirst() >= settings.windowMillis()) {
                seenAt.removeFirst();
            }
            if (seenAt.size() == REMEMBERED_PER_KIND) {
                seenAt.removeFirst();
            }
            seenAt.addLast(nowMillis);
            int count = seenAt.size();
            boolean strikes = count >= settings.alertThreshold()
                    && (!struck || nowMillis - struckAt >= settings.strikeIntervalMillis());
            if (strikes) {
                struck = true;
                struckAt = nowMillis;
            }
            return new TallyReading(count, strikes);
        }
    }
}
