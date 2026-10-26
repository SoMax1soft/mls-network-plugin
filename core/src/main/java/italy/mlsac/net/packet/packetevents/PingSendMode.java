// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import java.util.Locale;

enum PingSendMode {
    BASELINE("baseline"),
    LOOP("loop"),
    BATCH("batch");

    static final String PROPERTY = "mlsac.ping.send";
    static final PingSendMode DEFAULT = BATCH;

    private final String key;

    PingSendMode(String key) {
        this.key = key;
    }

    String key() {
        return key;
    }

    static PingSendMode fromSystemProperty() {
        return parse(System.getProperty(PROPERTY));
    }

    static PingSendMode parse(String raw) {
        if (raw == null) {
            return DEFAULT;
        }
        String wanted = raw.trim().toLowerCase(Locale.ROOT);
        for (PingSendMode mode : values()) {
            if (mode.key.equals(wanted)) {
                return mode;
            }
        }
        return DEFAULT;
    }

    PingDelivery delivery(ReachableChannels channels, PingWrite write) {
        return switch (this) {
            case BASELINE -> new DirectPings(write);
            case LOOP -> new LoopPings(channels, write);
            case BATCH -> new BurstPings(channels, write);
        };
    }
}
