// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.scoring.ModelResult;
import italy.mlsac.net.testkit.BundledResources;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NametagSightTest implements NametagPorts {

    private static final GameRelease V1_21 = new GameRelease(1, 21, 4);
    private final Map<UUID, HistoryView> histories = new HashMap<>();
    private final Set<UUID> staff = new HashSet<>();
    private final Map<UUID, List<SeedSighting>> seeds = new HashMap<>();
    private final Map<UUID, List<NametagPacket>> sent = new HashMap<>();
    private final Map<UUID, SeedSighting> ownPlaces = new HashMap<>();
    private NametagSettings settings = NametagSettings.pluginDefaults();
    private final UUID viewer = UUID.nameUUIDFromBytes("Mod".getBytes());
    private final UUID suspect = UUID.nameUUIDFromBytes("Cheater".getBytes());
    private static final int SUSPECT_ENTITY = 77;

    private NametagDirector director(GameRelease server) {
        return new NametagDirector(() -> settings, server, new NametagText(BundledResources.hex()),
                new SuspectTags(new NametagIdentities()), this);
    }

    private NametagDirector started(GameRelease server, Optional<GameRelease> client) {
        staff.add(viewer);
        histories.put(suspect, new HistoryView(3, List.of(new ModelResult("ultra", 0.912),
                new ModelResult("ultra", 0.97), new ModelResult("ultra", 0.4))));
        NametagDirector director = director(server);
        director.joined(viewer, 5, client, SuspectMode.PLAYING);
        director.joined(suspect, SUSPECT_ENTITY, Optional.empty(), SuspectMode.PLAYING);
        director.periodic();
        return director;
    }

    private ViewerSight sight(NametagDirector director) {
        return director.sight(viewer).orElseThrow();
    }

    private List<NametagPacket> sentTo(UUID player) {
        return sent.getOrDefault(player, List.of());
    }

    @Test
    void textDisplayRidesTheSuspectOnModernClients() {
        NametagDirector director = started(V1_21, Optional.of(V1_21));
        List<NametagPacket> packets = sight(director).spawned(SUSPECT_ENTITY, 1, 64, 2, look());
        assertEquals(3, packets.size());
        NametagPacket.SpawnTextDisplay spawn = assertInstanceOf(NametagPacket.SpawnTextDisplay.class, packets.get(0));
        assertEquals(2147483645, spawn.entityNumber());
        assertEquals(65.8, spawn.y(), 1e-9);
        NametagPacket.Metadata metadata = assertInstanceOf(NametagPacket.Metadata.class, packets.get(1));
        assertEquals(List.of(11, 12, 15, 17, 23, 24, 25, 27), metadata.entries().stream().map(MetaEntry::index).toList());
        assertEquals(new MetaValue.Vector3fValue(0f, 0.72f, 0f), metadata.entries().get(0).value());
        assertEquals(new MetaValue.TextValue("&f&lAVG: &c76% &7| &cU&f91% &cU&f97% &cU&f40%"),
                metadata.entries().get(4).value());
        assertEquals(new MetaValue.FloatValue(30 / 64f), metadata.entries().get(3).value());
        assertEquals(new NametagPacket.Passengers(SUSPECT_ENTITY, List.of(2147483645)), packets.get(2));
    }

    @Test
    void horseMountKeepsTheTagInThePassengerList() {
        NametagDirector director = started(V1_21, Optional.of(V1_21));
        sight(director).spawned(SUSPECT_ENTITY, 1, 64, 2, look());
        assertEquals(Optional.of(List.of(900, 2147483645)), sight(director).passengers(SUSPECT_ENTITY, List.of(900)));
        assertEquals(List.of(new NametagPacket.Passengers(SUSPECT_ENTITY, List.of(900, 2147483645))),
                sight(director).repairPassengers());
    }

    @Test
    void preShiftServerUsesOriginalIndicesAndLift() {
        NametagDirector director = started(new GameRelease(1, 20, 1), Optional.empty());
        List<NametagPacket> packets = sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        NametagPacket.Metadata metadata = (NametagPacket.Metadata) packets.get(1);
        assertEquals(List.of(10, 11, 14, 16, 22, 23, 24, 26), metadata.entries().stream().map(MetaEntry::index).toList());
        assertEquals(new MetaValue.Vector3fValue(0f, 1.15f, 0f), metadata.entries().get(0).value());
    }

    @Test
    void oldClientGetsFollowingArmorStand() {
        NametagDirector director = started(V1_21, Optional.of(new GameRelease(1, 19, 3)));
        List<NametagPacket> packets = sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        NametagPacket.SpawnArmorStand stand = assertInstanceOf(NametagPacket.SpawnArmorStand.class, packets.get(0));
        assertEquals(66.3, stand.y(), 1e-9);
        assertEquals(SpawnPacketKind.SPAWN_ENTITY, stand.kind());
        assertEquals(List.of(new NametagPacket.RelativeMove(stand.entityNumber(), 0.5, 0, -0.25)),
                sight(director).movedBy(SUSPECT_ENTITY, 0.5, 0, -0.25));
        assertEquals(List.of(new NametagPacket.Teleport(stand.entityNumber(), 0.5, 64 + 1.95, -0.25)),
                sight(director).posed(SUSPECT_ENTITY, SuspectPose.SNEAKING));
        assertEquals(List.of(new NametagPacket.Teleport(stand.entityNumber(), 3, 70 + 1.95, 3)),
                sight(director).movedTo(SUSPECT_ENTITY, 3, 70, 3));
        assertTrue(sight(director).passengers(SUSPECT_ENTITY, List.of(1)).isEmpty());
    }

    @Test
    void flyingSuspectKeepsItsTag() {
        NametagDirector director = started(V1_21, Optional.of(new GameRelease(1, 16, 5)));
        double feetY = 100;
        List<NametagPacket> spawn = sight(director).spawned(SUSPECT_ENTITY, 0, feetY, 0, look());
        int tagId = ((NametagPacket.SpawnArmorStand) spawn.get(0)).entityNumber();
        assertEquals(List.of(new NametagPacket.Teleport(tagId, 0, feetY + 1.0, 0)),
                sight(director).posed(SUSPECT_ENTITY, SuspectPose.fromGamePose("FALL_FLYING")));
        assertEquals(1, sight(director).movedBy(SUSPECT_ENTITY, 0, 5, 0).size());
        assertTrue(sight(director).reconcile(look()).isEmpty());
        assertEquals(1, sight(director).shownCount());
    }

    @Test
    void legacyServerUsesLivingSpawnAndOlderFlagsIndex() {
        NametagDirector director = started(new GameRelease(1, 16, 5), Optional.empty());
        List<NametagPacket> packets = sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        assertEquals(SpawnPacketKind.SPAWN_LIVING_ENTITY, ((NametagPacket.SpawnArmorStand) packets.get(0)).kind());
        NametagPacket.Metadata metadata = (NametagPacket.Metadata) packets.get(1);
        assertEquals(List.of(0, 2, 3, 14), metadata.entries().stream().map(MetaEntry::index).toList());
        assertEquals(13, ArmorStandRenderer.armorStandMaskSlot(new GameRelease(1, 14, 4)));
        assertEquals(11, ArmorStandRenderer.armorStandMaskSlot(new GameRelease(1, 12, 2)));
        assertEquals(15, ArmorStandRenderer.armorStandMaskSlot(new GameRelease(1, 17, 1)));
    }

    @Test
    void destroyRemovesTagsInTheSamePacket() {
        NametagDirector director = started(V1_21, Optional.of(V1_21));
        sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        DestroyReaction reaction = sight(director).destroyed(List.of(SUSPECT_ENTITY, 3));
        assertEquals(List.of(2147483645), reaction.idsToAppend());
        assertTrue(reaction.packetsAfter().isEmpty());
        assertEquals(0, sight(director).trackedCount());
    }

    @Test
    void server1170SendsSeparateDestroyPackets() {
        NametagDirector director = started(new GameRelease(1, 17, 0), Optional.empty());
        sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        DestroyReaction reaction = sight(director).destroyed(List.of(SUSPECT_ENTITY));
        assertTrue(reaction.idsToAppend().isEmpty());
        assertEquals(List.of(new NametagPacket.Destroy(List.of(2147483645))), reaction.packetsAfter());
    }

    @Test
    void respawnSpawnPacketReplacesOldTag() {
        NametagDirector director = started(V1_21, Optional.of(V1_21));
        sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        List<NametagPacket> again = sight(director).spawned(SUSPECT_ENTITY, 5, 64, 5, look());
        assertEquals(new NametagPacket.Destroy(List.of(2147483645)), again.get(0));
        assertInstanceOf(NametagPacket.SpawnTextDisplay.class, again.get(1));
        sight(director).viewReset(ClientStage.IN_WORLD);
        assertEquals(0, sight(director).trackedCount());
    }

    @Test
    void textChangeSendsOnlyTheTextIndex() {
        NametagDirector director = started(V1_21, Optional.of(V1_21));
        sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        histories.put(suspect, new HistoryView(4, List.of(new ModelResult("ultra", 0.5))));
        director.periodic();
        List<NametagPacket> packets = sentTo(viewer);
        assertEquals(1, packets.size());
        NametagPacket.Metadata metadata = (NametagPacket.Metadata) packets.get(0);
        assertEquals(List.of(23), metadata.entries().stream().map(MetaEntry::index).toList());
    }

    @Test
    void nonStaffViewerNeverReceivesTagsUntilGranted() {
        NametagDirector director = started(V1_21, Optional.of(V1_21));
        staff.remove(viewer);
        UUID other = UUID.randomUUID();
        director.joined(other, 9, Optional.of(V1_21), SuspectMode.PLAYING);
        assertTrue(director.sight(other).orElseThrow().spawned(SUSPECT_ENTITY, 0, 64, 0, look()).isEmpty());
        director.periodic();
        director.periodic();
        assertTrue(sentTo(other).isEmpty());
        staff.add(other);
        seeds.put(other, List.of(new SeedSighting(SUSPECT_ENTITY, 1, 64, 1)));
        director.periodic();
        director.periodic();
        assertInstanceOf(NametagPacket.SpawnTextDisplay.class, sentTo(other).get(0));
    }

    @Test
    void losingStaffRemovesShownTags() {
        NametagDirector director = started(V1_21, Optional.of(V1_21));
        sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        staff.remove(viewer);
        director.periodic();
        assertEquals(List.of(new NametagPacket.Destroy(List.of(2147483645))), sentTo(viewer));
        assertEquals(0, sight(director).trackedCount());
    }

    @Test
    void scaleChangeRespawnsWithNewLook() {
        NametagDirector director = started(V1_21, Optional.of(V1_21));
        sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        NametagSettings old = settings;
        settings = old.redisplayed(new NametagDisplay(true, 0, 25, true, false, 1.5, Billboard.CENTER));
        director.periodic();
        List<NametagPacket> packets = sentTo(viewer);
        assertEquals(new NametagPacket.Destroy(List.of(2147483645)), packets.get(0));
        NametagPacket.Metadata metadata = (NametagPacket.Metadata) packets.get(2);
        assertEquals(new MetaValue.Vector3fValue(1.5f, 1.5f, 1.5f), metadata.entries().get(1).value());
    }

    @Test
    void spectatorSuspectHasNoTagAndGetsItBackWhenLeaving() {
        NametagDirector director = started(V1_21, Optional.of(V1_21));
        sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        director.modeChanged(suspect, SuspectMode.SPECTATING);
        assertEquals(List.of(new NametagPacket.Destroy(List.of(2147483645))), sentTo(viewer));
        sent.clear();
        assertTrue(sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look()).isEmpty());
        director.periodic();
        assertTrue(sentTo(viewer).isEmpty());
        director.modeChanged(suspect, SuspectMode.PLAYING);
        assertInstanceOf(NametagPacket.SpawnTextDisplay.class, sentTo(viewer).get(0));
    }

    @Test
    void spectatorAtJoinGetsNoTag() {
        staff.add(viewer);
        histories.put(suspect, new HistoryView(1, List.of(new ModelResult("ultra", 0.9))));
        NametagDirector director = director(V1_21);
        director.joined(viewer, 5, Optional.of(V1_21), SuspectMode.PLAYING);
        director.joined(suspect, SUSPECT_ENTITY, Optional.empty(), SuspectMode.SPECTATING);
        director.periodic();
        assertTrue(sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look()).isEmpty());
    }

    @Test
    void stopDestroysEverythingShown() {
        NametagDirector director = started(V1_21, Optional.of(V1_21));
        sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        director.stop();
        assertEquals(List.of(new NametagPacket.Destroy(List.of(2147483645))), sentTo(viewer));
        assertTrue(director.sight(viewer).isEmpty());
    }

    private NametagLook look() {
        return new NametagLook(settings, 0);
    }

    @Override
    public Optional<HistoryView> history(UUID player) {
        return Optional.ofNullable(histories.get(player));
    }

    @Override
    public boolean staff(UUID player) {
        return staff.contains(player);
    }

    @Override
    public List<SeedSighting> currentlyVisibleTo(UUID player) {
        return seeds.getOrDefault(player, List.of());
    }

    @Override
    public Optional<SeedSighting> ownPlace(UUID player) {
        return Optional.ofNullable(ownPlaces.get(player));
    }

    @Override
    public void onViewerThread(UUID player, Runnable task) {
        task.run();
    }

    @Override
    public void send(UUID player, List<NametagPacket> packets) {
        sent.computeIfAbsent(player, key -> new ArrayList<>()).addAll(packets);
    }
}
