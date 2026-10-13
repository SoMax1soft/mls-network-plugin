// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.OptionalLong;
import java.util.function.LongSupplier;

public final class DossierTimes {

    static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    static final String UNKNOWN = "-";

    private final LongSupplier wallMillis;
    private final ZoneId zone;

    public DossierTimes(LongSupplier wallMillis, ZoneId zone) {
        this.wallMillis = wallMillis;
        this.zone = zone;
    }

    public String stamp(long epochMillis) {
        return STAMP.withZone(zone).format(Instant.ofEpochMilli(epochMillis));
    }

    public String stamp(OptionalLong epochMillis) {
        return epochMillis.isPresent() ? stamp(epochMillis.getAsLong()) : UNKNOWN;
    }

    public String ago(long epochMillis) {
        long seconds = Math.max(0, (wallMillis.getAsLong() - epochMillis) / 1000);
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        return hours < 48 ? hours + "h" : hours / 24 + "d";
    }

    public String ago(OptionalLong epochMillis) {
        return epochMillis.isPresent() ? ago(epochMillis.getAsLong()) : UNKNOWN;
    }
}
