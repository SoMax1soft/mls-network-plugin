// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.staff.DataRestoreSettings;
import italy.mlsac.net.pipeline.restore.RestoreLimits;
import java.util.function.Supplier;

public final class ConfiguredRestoreLimits implements Supplier<RestoreLimits> {

    private final ConfigReference config;
    private volatile Reading reading = Reading.UNREAD;

    public ConfiguredRestoreLimits(ConfigReference config) {
        this.config = config;
    }

    @Override
    public RestoreLimits get() {
        EffectiveConfig now = config.get();
        Reading known = reading;
        if (known.source() != now) {
            DataRestoreSettings settings = now.dataRestore();
            known = new Reading(now, new RestoreLimits(settings.enabled(), settings.maxPlayers(), settings.maxBytes()));
            reading = known;
        }
        return known.limits();
    }

    private record Reading(EffectiveConfig source, RestoreLimits limits) {
        static final Reading UNREAD = new Reading(null, RestoreLimits.OFF);
    }
}
