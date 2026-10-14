// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.integrity;

import italy.mlsac.net.config.integrity.BadPacketSettings;
import italy.mlsac.net.detection.BadPacketObserved;
import italy.mlsac.net.logbook.Logbook;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.SuspectRoster;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class BadPacketWatch {

    private final BadPacketTally tally;
    private final Supplier<BadPacketSettings> settings;
    private final BooleanSupplier detectionEnabled;
    private final LongSupplier millis;
    private final Consumer<Runnable> background;
    private final BadPacketStrike strike;
    private final SuspectRoster roster;
    private final Logbook log;

    public BadPacketWatch(BadPacketTally tally, Supplier<BadPacketSettings> settings, BooleanSupplier detectionEnabled,
                          LongSupplier millis, Consumer<Runnable> background, BadPacketStrike strike,
                          SuspectRoster roster, Logbook log) {
        this.tally = tally;
        this.settings = settings;
        this.detectionEnabled = detectionEnabled;
        this.millis = millis;
        this.background = background;
        this.strike = strike;
        this.roster = roster;
        this.log = log;
    }

    public void observed(BadPacketObserved event) {
        BadPacketSettings current = settings.get();
        if (!current.enabled() || !detectionEnabled.getAsBoolean()) {
            return;
        }
        TallyReading reading = tally.observe(event.player(), event.kind(), millis.getAsLong(), current);
        if (reading.strikes()) {
            background.accept(() -> strike.strike(event.player(), event.kind(), reading.count(),
                    current.windowSeconds()));
        } else if (reading.count() < current.alertThreshold()) {
            log.debug("[Detection] Bad packets: " + event.kind().slug() + " from " + nick(event.player()) + " ("
                    + reading.count() + "/" + current.windowSeconds() + "s, alert at " + current.alertThreshold()
                    + ")");
        }
    }

    public void left(UUID player) {
        tally.forget(player);
    }

    public void clear() {
        tally.clear();
    }

    private String nick(UUID player) {
        return roster.retained(player).map(SuspectRecord::name).orElse(player.toString());
    }
}
