// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

import java.util.Optional;

public final class ClientVersionRules {

    private static final GameVersion V1_9 = GameVersion.of(1, 9, 0);
    private static final GameVersion V1_14_4 = GameVersion.of(1, 14, 4);
    private static final GameVersion V1_17 = GameVersion.of(1, 17, 0);
    private static final GameVersion V1_19_3 = GameVersion.of(1, 19, 3);
    private static final GameVersion V1_19_4 = GameVersion.of(1, 19, 4);
    private static final GameVersion V1_20_2 = GameVersion.of(1, 20, 2);
    private static final GameVersion V1_21 = GameVersion.of(1, 21, 0);
    private static final GameVersion V1_21_2 = GameVersion.of(1, 21, 2);
    private static final GameVersion V1_21_4 = GameVersion.of(1, 21, 4);
    private static final GameVersion V1_21_9 = GameVersion.of(1, 21, 9);

    private final Optional<GameVersion> client;

    public ClientVersionRules(Optional<GameVersion> client) {
        this.client = client;
    }

    public Optional<GameVersion> client() {
        return client;
    }

    public boolean transactionCompensation() {
        return client.map(v -> v.atLeast(V1_17)).orElse(true);
    }

    public boolean duplicateMovementFilter() {
        return client.map(v -> v.atLeast(V1_17) && v.below(V1_21)).orElse(false);
    }

    public boolean clientTickEnd() {
        return client.map(v -> v.atLeast(V1_21_2)).orElse(false);
    }

    public boolean modernDeltaEncoding() {
        return client.map(v -> v.atLeast(V1_19_3)).orElse(true);
    }

    public boolean maySkipTicks() {
        return client.map(v -> v.atLeast(V1_9)).orElse(true);
    }

    public boolean viewStalls() {
        return client.map(v -> strictlyBetween(v, V1_14_4, V1_20_2) || strictlyBetween(v, V1_21_4, V1_21_9))
                .orElse(false);
    }

    public boolean textDisplays() {
        return client.map(v -> v.atLeast(V1_19_4)).orElse(true);
    }

    private static boolean strictlyBetween(GameVersion v, GameVersion low, GameVersion high) {
        return v.compareTo(low) > 0 && v.compareTo(high) < 0;
    }
}
