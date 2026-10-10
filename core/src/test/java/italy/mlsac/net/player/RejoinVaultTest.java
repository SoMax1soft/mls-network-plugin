// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RejoinVaultTest {

    @Test
    void stateSurvivesFifteenMinutesExactly() {
        RejoinVault<String> vault = new RejoinVault<>(RejoinVault.OWNER_RETENTION);
        UUID player = UUID.randomUUID();
        vault.keep(player, "buffer=12", 0);
        assertEquals(Optional.of("buffer=12"), vault.reclaim(player, 15 * 60 * 1000));
        assertTrue(vault.reclaim(player, 15 * 60 * 1000).isEmpty());
        vault.keep(player, "late", 0);
        assertTrue(vault.reclaim(player, 15 * 60 * 1000 + 1).isEmpty());
        vault.keep(player, "old", 0);
        assertEquals(1, vault.purgeExpired(16 * 60 * 1000));
        assertEquals(0, vault.size());
    }
}
