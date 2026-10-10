// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VersionRulesTest {

    private static ClientVersionRules client(String version) {
        return new ClientVersionRules(GameVersion.parse(version));
    }

    private static ServerVersionRules server(String version) {
        return new ServerVersionRules(GameVersion.parse(version).orElseThrow());
    }

    @Test
    void duplicateMovementFilterBounds() {
        assertFalse(client("1.16.5").duplicateMovementFilter());
        assertTrue(client("1.17").duplicateMovementFilter());
        assertTrue(client("1.20.6").duplicateMovementFilter());
        assertFalse(client("1.21").duplicateMovementFilter());
        assertFalse(new ClientVersionRules(Optional.empty()).duplicateMovementFilter());
    }

    @Test
    void unknownClientGetsModernProfile() {
        ClientVersionRules unknown = new ClientVersionRules(Optional.empty());
        assertTrue(unknown.modernDeltaEncoding());
        assertTrue(unknown.maySkipTicks());
        assertFalse(unknown.viewStalls());
        assertFalse(unknown.clientTickEnd());
        assertTrue(unknown.transactionCompensation());
        assertTrue(unknown.textDisplays());
    }

    @Test
    void clientSpecificSwitches() {
        assertFalse(client("1.16.5").transactionCompensation());
        assertTrue(client("1.21.2").clientTickEnd());
        assertFalse(client("1.21.1").clientTickEnd());
        assertFalse(client("1.14.4").viewStalls());
        assertTrue(client("1.16.5").viewStalls());
        assertFalse(client("1.20.2").viewStalls());
        assertTrue(client("1.21.5").viewStalls());
        assertFalse(client("1.21.9").viewStalls());
    }

    @Test
    void serverSwitches() {
        assertEquals(0, server("1.20.1").textDisplayIndexShift());
        assertEquals(1, server("1.20.2").textDisplayIndexShift());
        assertEquals(1.15, server("1.19.4").textDisplayLift());
        assertEquals(0.72, server("1.21.4").textDisplayLift());
        assertFalse(server("1.19.3").textDisplaysAvailable());
        assertTrue(server("1.19.4").textDisplaysAvailable());
        assertTrue(server("1.18.2").spawnLivingEntityPacket());
        assertFalse(server("1.19").spawnLivingEntityPacket());
        assertEquals(15, server("1.17").armorStandFlagsIndex());
        assertEquals(14, server("1.16.5").armorStandFlagsIndex());
        assertTrue(server("1.17").destroyCarriesSingleId());
        assertFalse(server("1.17.1").destroyCarriesSingleId());
        assertTrue(server("1.21.2").positionSyncPacket());
        assertTrue(server("1.20.5").modernRegistryNames());
    }

    @Test
    void registryAliasesPointToTheRunningSide() {
        assertEquals(List.of("SLOW", "SLOWNESS"), new RegistryNameAliases(server("1.21.4")).candidates("slow"));
        assertEquals(List.of("SLOWNESS", "SLOW"), new RegistryNameAliases(server("1.20.4")).candidates("SLOWNESS"));
        assertEquals(List.of("SPEED"), new RegistryNameAliases(server("1.21.4")).candidates("SPEED"));
    }
}
