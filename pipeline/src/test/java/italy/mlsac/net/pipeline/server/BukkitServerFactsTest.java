// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.packet.ClientVersions;
import italy.mlsac.net.pipeline.compensation.ViewSeed;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.platform.GameVersion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class BukkitServerFactsTest {

    private static final UUID WORLD = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-00000018e0cf");
    private static final UUID VIEWER = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-00000002483a");
    private static final UUID OTHER = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-000000026729");

    private final World world = PlayerStandIns.world(WORLD, "arena");
    private final Map<UUID, String> versionNames = new HashMap<>();
    private final Player viewer = PlayerStandIns.standing(VIEWER, 21, world, -9.5, -60.0, 4.5).build();
    private final Player other = PlayerStandIns.standing(OTHER, 22, world, 8.5, -60.0, -3.5).build();

    @Test
    void noConnectionInfoMeansNoSeed() {
        assertNull(facts(FixedOwnership.everything()).viewSeed(VIEWER));
    }

    @Test
    void olderClientsGetOnlyTheirVersion() {
        versionNames.put(VIEWER, "V_1_16_4");
        ViewSeed seed = facts(FixedOwnership.everything()).viewSeed(VIEWER);
        assertEquals("V_1_16_4", seed.clientVersion());
        assertNull(seed.world());
        assertNull(seed.position());
        assertTrue(seed.others().isEmpty());
    }

    @Test
    void modernClientsSeeEveryOnlinePlayerInServerOrder() {
        versionNames.put(VIEWER, "V_1_21_4");
        ViewSeed seed = facts(FixedOwnership.everything()).viewSeed(VIEWER);
        assertEquals("V_1_21_4", seed.clientVersion());
        assertEquals(WORLD, seed.world());
        assertEquals(new Vec3(-9.5, -60.0, 4.5), seed.position());
        assertEquals(List.of(new ViewSeed.SeenPlayer(VIEWER, 21, WORLD, new Vec3(-9.5, -60.0, 4.5)),
                new ViewSeed.SeenPlayer(OTHER, 22, WORLD, new Vec3(8.5, -60.0, -3.5))), seed.others());
    }

    @Test
    void playersOfOtherRegionsComeFromTheirLastTick() {
        versionNames.put(VIEWER, "V_1_20_3");
        FixedOwnership ownership = FixedOwnership.everything().withoutEntity(other);
        PlaceBook places = new PlaceBook(ownership);
        BukkitServerFacts facts = new BukkitServerFacts(roster(), clients(), places);
        assertEquals(1, facts.viewSeed(VIEWER).others().size());
        places.perform(other, 1L);
        assertEquals(2, facts.viewSeed(VIEWER).others().size());
        places.forget(OTHER);
        assertEquals(1, facts.viewSeed(VIEWER).others().size());
    }

    @Test
    void viewerOfAnotherRegionWithoutHistoryGetsVersionOnly() {
        versionNames.put(VIEWER, "V_1_21_4");
        ViewSeed seed = facts(FixedOwnership.everything().withoutEntity(viewer)).viewSeed(VIEWER);
        assertNull(seed.world());
        assertTrue(seed.others().isEmpty());
    }

    @Test
    void entityNumbersResolveToOnlinePlayers() {
        BukkitServerFacts facts = facts(FixedOwnership.everything());
        assertEquals(OTHER, facts.playerByEntity(22));
        assertEquals(VIEWER, facts.playerByEntity(21));
        assertNull(facts.playerByEntity(999));
    }

    @Test
    void validMeansOnline() {
        BukkitServerFacts facts = facts(FixedOwnership.everything());
        assertTrue(facts.valid(VIEWER));
        assertFalse(facts.valid(UUID.randomUUID()));
    }

    private BukkitServerFacts facts(FixedOwnership ownership) {
        return new BukkitServerFacts(roster(), clients(), new PlaceBook(ownership));
    }

    private FixedRoster roster() {
        return new FixedRoster().with(viewer).with(other);
    }

    private ClientVersions clients() {
        return new ClientVersions() {
            @Override
            public Optional<GameVersion> version(UUID player) {
                return Optional.empty();
            }

            @Override
            public Optional<String> versionName(UUID player) {
                return Optional.ofNullable(versionNames.get(player));
            }

            @Override
            public OptionalInt protocol(UUID player) {
                return OptionalInt.empty();
            }
        };
    }
}
