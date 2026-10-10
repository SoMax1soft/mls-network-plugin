// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

import java.util.Objects;

public final class ServerVersionRules {

    private static final GameVersion V1_14 = GameVersion.of(1, 14, 0);
    private static final GameVersion V1_15 = GameVersion.of(1, 15, 0);
    private static final GameVersion V1_17 = GameVersion.of(1, 17, 0);
    private static final GameVersion V1_19 = GameVersion.of(1, 19, 0);
    private static final GameVersion V1_19_4 = GameVersion.of(1, 19, 4);
    private static final GameVersion V1_20_2 = GameVersion.of(1, 20, 2);
    private static final GameVersion V1_20_5 = GameVersion.of(1, 20, 5);
    private static final GameVersion V1_21_2 = GameVersion.of(1, 21, 2);

    private final GameVersion server;

    public ServerVersionRules(GameVersion server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public GameVersion server() {
        return server;
    }

    public boolean modernRegistryNames() {
        return server.atLeast(V1_20_5);
    }

    public boolean spawnLivingEntityPacket() {
        return server.below(V1_19);
    }

    public boolean textDisplaysAvailable() {
        return server.atLeast(V1_19_4);
    }

    public int textDisplayIndexShift() {
        return server.atLeast(V1_20_2) ? 1 : 0;
    }

    public double textDisplayLift() {
        return server.atLeast(V1_20_2) ? 0.72 : 1.15;
    }

    public int armorStandFlagsIndex() {
        if (server.atLeast(V1_17)) {
            return 15;
        }
        if (server.atLeast(V1_15)) {
            return 14;
        }
        return server.atLeast(V1_14) ? 13 : 11;
    }

    public boolean destroyCarriesSingleId() {
        return server.equals(V1_17);
    }

    public boolean playersUseGenericSpawn() {
        return server.atLeast(V1_20_2);
    }

    public boolean configurationPhase() {
        return server.atLeast(V1_20_2);
    }

    public boolean positionSyncPacket() {
        return server.atLeast(V1_21_2);
    }
}
