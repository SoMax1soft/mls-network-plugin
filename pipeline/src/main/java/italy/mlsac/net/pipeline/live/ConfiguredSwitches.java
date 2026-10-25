// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.backend.BackendGateway;
import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.detection.choice.ModelRoute;
import italy.mlsac.net.detection.choice.PlayerModelRoutes;
import italy.mlsac.net.pipeline.environment.BackendAvailability;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import java.util.UUID;

public final class ConfiguredSwitches implements DetectionSwitches, BackendAvailability {

    private final ConfigReference config;
    private final BackendGateway backend;
    private final PlayerModelRoutes routes;
    private volatile SwitchReading reading = SwitchReading.UNREAD;

    public ConfiguredSwitches(ConfigReference config, BackendGateway backend) {
        this(config, backend, PlayerModelRoutes.SERVER_DEFAULT);
    }

    public ConfiguredSwitches(ConfigReference config, BackendGateway backend, PlayerModelRoutes routes) {
        this.config = config;
        this.backend = backend;
        this.routes = routes;
    }

    @Override
    public boolean detectionEnabled() {
        return current().detection();
    }

    @Override
    public boolean ultraEnabled() {
        return current().ultra() || routes.anyPlayerOn(ModelRoute.ULTRA);
    }

    @Override
    public boolean ultraEnabledFor(UUID player) {
        return routes.routeOf(player).runsUltra(current().ultra());
    }

    @Override
    public boolean legacyEnabledFor(UUID player) {
        return routes.routeOf(player).runsLegacy(current().ultra());
    }

    @Override
    public int wallChecksPerSecond() {
        return current().wallChecks();
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
            known = new SwitchReading(now, now.detection().enabled(), now.detection().ultraMode(),
                    now.core().wallChecksPerSecond());
            reading = known;
        }
        return known;
    }

    private record SwitchReading(EffectiveConfig source, boolean detection, boolean ultra, int wallChecks) {
        static final SwitchReading UNREAD = new SwitchReading(null, false, false, 10);
    }
}
