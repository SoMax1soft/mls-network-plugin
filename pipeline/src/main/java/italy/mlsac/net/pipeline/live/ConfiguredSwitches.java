// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.backend.BackendGateway;
import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.pipeline.environment.BackendAvailability;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;

public final class ConfiguredSwitches implements DetectionSwitches, BackendAvailability {

    private final ConfigReference config;
    private final BackendGateway backend;
    private volatile SwitchReading reading = SwitchReading.UNREAD;

    public ConfiguredSwitches(ConfigReference config, BackendGateway backend) {
        this.config = config;
        this.backend = backend;
    }

    @Override
    public boolean detectionEnabled() {
        return current().detection();
    }

    @Override
    public boolean ultraEnabled() {
        return current().ultra();
    }

    @Override
    public boolean clientPresent() {
        return backend.exists();
    }

    @Override
    public boolean usable() {
        return backend.usable();
    }

    private SwitchReading current() {
        EffectiveConfig now = config.get();
        SwitchReading known = reading;
        if (known.source() != now) {
            known = new SwitchReading(now, now.detection().enabled(), now.detection().ultraMode());
            reading = known;
        }
        return known;
    }

    private record SwitchReading(EffectiveConfig source, boolean detection, boolean ultra) {
        static final SwitchReading UNREAD = new SwitchReading(null, false, false);
    }
}
