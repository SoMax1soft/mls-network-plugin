// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.packet.ClientVersions;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.testkit.FakePlayers;
import italy.mlsac.net.testkit.ManualScheduler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientCensusTest {

    private final ManualScheduler scheduler = new ManualScheduler();
    private final FakePlayers players = new FakePlayers();
    private final Map<UUID, Integer> protocols = new HashMap<>();
    private final Map<UUID, String> brands = new HashMap<>();

    @Test
    void unattachedCensusReportsNothing() {
        players.add("Alice");
        players.add("Bob");
        ClientCensus census = new ClientCensus(scheduler, players);
        assertSame(ClientDistribution.EMPTY, census.get());
        assertTrue(census.sample().empty());
    }

    @Test
    void platformWithoutBrandSupportStillReportsProtocols() {
        UUID alice = add("Alice", 765, null);
        ClientCensus census = new ClientCensus(scheduler, players);
        census.attach(versions(), ClientBrands.NONE);
        ClientDistribution sample = census.sample();
        assertEquals(Map.of("765", 1), sample.protocols());
        assertTrue(sample.brands().isEmpty());
        assertEquals(OptionalInt.of(765), versions().protocol(alice));
    }

    @Test
    void packetLayerWithoutVersionsStillReportsBrands() {
        add("Alice", null, "vanilla");
        ClientCensus census = new ClientCensus(scheduler, players);
        census.attach(ClientVersions.NONE, brandSource());
        ClientDistribution sample = census.sample();
        assertTrue(sample.protocols().isEmpty());
        assertEquals(Map.of("vanilla", 1), sample.brands());
    }

    @Test
    void countsAreAggregatedNeverPerPlayer() {
        add("Alice", 765, "vanilla");
        add("Bob", 765, "vanilla");
        add("Carol", 47, "lunarclient:v3");
        ClientCensus census = new ClientCensus(scheduler, players);
        census.attach(versions(), brandSource());
        ClientDistribution sample = census.sample();
        assertEquals(List.of("765", "47"), new ArrayList<>(sample.protocols().keySet()));
        assertEquals(Map.of("765", 2, "47", 1), sample.protocols());
        assertEquals(Map.of("vanilla", 2, "lunarclient:v3", 1), sample.brands());
    }

    @Test
    void onlyTheTopEntriesAreReported() {
        for (int index = 0; index < ClientCensus.REPORTED_ENTRIES + 4; index++) {
            add("Player" + index, 700 + index, "brand" + index);
        }
        add("Loudest", 999, "loudest");
        add("Loudest2", 999, "loudest");
        ClientCensus census = new ClientCensus(scheduler, players);
        census.attach(versions(), brandSource());
        ClientDistribution sample = census.sample();
        assertEquals(ClientCensus.REPORTED_ENTRIES, sample.protocols().size());
        assertEquals(ClientCensus.REPORTED_ENTRIES, sample.brands().size());
        assertEquals("999", sample.protocols().keySet().iterator().next());
        assertEquals("loudest", sample.brands().keySet().iterator().next());
    }

    @Test
    void brandNamesAreSanitisedAndBounded() {
        assertEquals("vanilla", ClientCensus.brandKey("  Vanilla "));
        assertEquals("fabric_1.21", ClientCensus.brandKey("fabric 1.21"));
        assertEquals(ClientCensus.UNNAMED_BRAND, ClientCensus.brandKey("   "));
        assertEquals(ClientCensus.UNNAMED_BRAND, ClientCensus.brandKey("\u0001\u0002"));
        assertEquals(ClientCensus.BRAND_LENGTH_LIMIT, ClientCensus.brandKey("a".repeat(120)).length());
    }

    @Test
    void stoppingClearsTheSnapshot() {
        add("Alice", 765, "vanilla");
        ClientCensus census = new ClientCensus(scheduler, players);
        census.attach(versions(), brandSource());
        census.start();
        assertEquals(Map.of("765", 1), census.get().protocols());
        census.stop();
        assertSame(ClientDistribution.EMPTY, census.get());
    }

    private UUID add(String name, Integer protocol, String brand) {
        UUID id = players.add(name).id();
        if (protocol != null) {
            protocols.put(id, protocol);
        }
        if (brand != null) {
            brands.put(id, brand);
        }
        return id;
    }

    private ClientVersions versions() {
        return new ClientVersions() {

            @Override
            public Optional<GameVersion> version(UUID player) {
                return Optional.empty();
            }

            @Override
            public Optional<String> versionName(UUID player) {
                return Optional.empty();
            }

            @Override
            public OptionalInt protocol(UUID player) {
                Integer known = protocols.get(player);
                return known == null ? OptionalInt.empty() : OptionalInt.of(known);
            }
        };
    }

    private ClientBrands brandSource() {
        return player -> Optional.ofNullable(brands.get(player));
    }
}
