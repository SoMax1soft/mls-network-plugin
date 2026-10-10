// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

import java.util.Objects;

public record PlatformProfile(ServerSoftware software, VersionResolution serverVersion,
                              boolean tickEndEventAvailable, int javaFeatureVersion) {

    static final String TICK_END_EVENT = "com.destroystokyo.paper.event.server.ServerTickEndEvent";

    public PlatformProfile {
        Objects.requireNonNull(software, "software");
        Objects.requireNonNull(serverVersion, "serverVersion");
    }

    public static PlatformProfile detect(ClassProbe probe, String bukkitVersion, int javaFeatureVersion) {
        ServerSoftware software = ServerSoftware.detect(probe);
        VersionResolution version = new ServerVersionTable().resolve(bukkitVersion);
        return new PlatformProfile(software, version, probe.isPresent(TICK_END_EVENT), javaFeatureVersion);
    }

    public boolean folia() {
        return software == ServerSoftware.FOLIA;
    }

    public int updateChannel() {
        return javaFeatureVersion >= 21 ? 21 : 17;
    }
}
