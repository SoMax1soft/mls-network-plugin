// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.nametag;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.nametag.GameRelease;
import italy.mlsac.net.nametag.NametagDirector;
import italy.mlsac.net.nametag.NametagIdentities;
import italy.mlsac.net.nametag.NametagPacket;
import italy.mlsac.net.nametag.NametagPorts;
import italy.mlsac.net.nametag.NametagSettings;
import italy.mlsac.net.nametag.NametagText;
import italy.mlsac.net.nametag.OwnTag;
import italy.mlsac.net.nametag.SeedSighting;
import italy.mlsac.net.nametag.SuspectMode;
import italy.mlsac.net.nametag.SuspectTags;
import italy.mlsac.net.nametag.TagLayout;
import italy.mlsac.net.packet.EntityDestroyPacket;
import italy.mlsac.net.packet.EntityMetadataPacket;
import italy.mlsac.net.packet.EntityMovePacket;
import italy.mlsac.net.packet.EntitySpawnPacket;
import italy.mlsac.net.packet.ListenerOrder;
import italy.mlsac.net.packet.MoveKind;
import italy.mlsac.net.packet.MovementPacket;
import italy.mlsac.net.packet.PassengersPacket;
import italy.mlsac.net.packet.PoseKind;
import italy.mlsac.net.packet.ResetCause;
import italy.mlsac.net.packet.SpawnKind;
import italy.mlsac.net.packet.ViewResetPacket;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.scoring.ModelResult;
import italy.mlsac.net.testkit.FakePacketLayer;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.text.LegacyColorizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NametagPacketFollowerTest implements NametagPorts {

    private static final GameRelease SERVER = new GameRelease(1, 21, 4);
    private static final int SUSPECT_ENTITY = 77;
    private static final int TAG = 2147483646;

    private final FakePacketLayer packets = new FakePacketLayer(GameVersion.of(1, 21, 4));
    private final NametagWire wire = new NametagWire(packets.sender(), new LegacyColorizer(new HexColorNormalizer()));
    private final UUID viewer = UUID.nameUUIDFromBytes("Mod".getBytes());
    private final UUID suspect = UUID.nameUUIDFromBytes("Cheater".getBytes());
    private final Set<UUID> staff = new HashSet<>(Set.of(viewer));
    private final Map<UUID, SeedSighting> ownPlaces = new HashMap<>();
    private NametagSettings settings = NametagSettings.pluginDefaults();
    private final NametagDirector director = new NametagDirector(() -> settings, SERVER,
            new NametagText(new HexColorNormalizer()), new SuspectTags(new NametagIdentities()), this);
    private final NametagPacketFollower follower = new NametagPacketFollower(() -> Optional.of(director), wire);

    private void started(Optional<GameRelease> viewerClient) {
        director.joined(suspect, SUSPECT_ENTITY, Optional.empty(), SuspectMode.PLAYING);
        director.joined(viewer, 5, viewerClient, SuspectMode.PLAYING);
        director.periodic();
        follower.listen(packets);
    }

    private EntitySpawnPacket spawnOfSuspect() {
        return new EntitySpawnPacket(viewer, SUSPECT_ENTITY, Optional.of(suspect), SpawnKind.PLAYER, true, 1, 64, 2);
    }

    @Test
    void aTextDisplayRidesTheSuspectRightAfterItsSpawn() {
        started(Optional.of(SERVER));
        packets.send(spawnOfSuspect());
        List<String> sent = packets.sent();
        assertEquals(4, sent.size());
        assertTrue(sent.get(0).contains("entityNumber=" + TAG) && sent.get(0).contains("TEXT_DISPLAY"));
        assertTrue(sent.get(0).contains("y=65.8"));
        assertTrue(sent.get(1).startsWith("metadata " + viewer + " " + TAG));
        assertEquals("passengers " + viewer + " " + SUSPECT_ENTITY + " [" + TAG + "]", sent.get(2));
        assertEquals("flush " + viewer, sent.get(3));
    }

    @Test
    void theTagLeavesInTheSameDestroyPacketAsItsSuspect() {
        started(Optional.of(SERVER));
        packets.send(spawnOfSuspect());
        EntityDestroyPacket sent = packets.send(new EntityDestroyPacket(viewer, new int[] {SUSPECT_ENTITY, 9}));
        assertArrayEquals(new int[] {SUSPECT_ENTITY, 9, TAG}, sent.entityNumbers());
    }

    @Test
    void passengerListsFromTheServerKeepTheTag() {
        started(Optional.of(SERVER));
        packets.send(spawnOfSuspect());
        PassengersPacket sent = packets.send(new PassengersPacket(viewer, SUSPECT_ENTITY, new int[] {40}));
        assertArrayEquals(new int[] {40, TAG}, sent.passengers());
    }

    @Test
    void cancelledSpawnsAreRespected() {
        started(Optional.of(SERVER));
        packets.onOutbound(EntitySpawnPacket.class, ListenerOrder.NORMAL, context -> context.cancel());
        packets.send(spawnOfSuspect());
        assertTrue(packets.sent().isEmpty());
    }

    @Test
    void nonStaffViewersNeverGetNametagPackets() {
        staff.clear();
        started(Optional.of(SERVER));
        packets.send(spawnOfSuspect());
        assertTrue(packets.sent().isEmpty());
    }

    @Test
    void armourStandsFollowMovesTeleportsAndPoses() {
        started(Optional.of(new GameRelease(1, 19, 2)));
        packets.send(spawnOfSuspect());
        int afterSpawn = packets.sent().size();
        packets.send(new EntityMovePacket(viewer, SUSPECT_ENTITY, MoveKind.RELATIVE, 0.5, 0, -0.25, true));
        packets.send(new EntityMovePacket(viewer, SUSPECT_ENTITY, MoveKind.ROTATION_ONLY, 0, 0, 0, true));
        packets.send(new EntityMovePacket(viewer, SUSPECT_ENTITY, MoveKind.TELEPORT, 10, 70, 10, false));
        packets.send(new EntityMetadataPacket(viewer, SUSPECT_ENTITY, Optional.of(PoseKind.CROUCHING)));
        List<String> after = packets.sent().subList(afterSpawn, packets.sent().size());
        assertEquals("move " + viewer + " " + TAG + " 0.5 0.0 -0.25", after.get(0));
        assertEquals("flush " + viewer, after.get(1));
        assertEquals("teleport " + viewer + " " + TAG + " 10.0 72.3 10.0", after.get(2));
        assertEquals("teleport " + viewer + " " + TAG + " 10.0 71.95 10.0", after.get(4));
    }

    @Test
    void spectatorModeRemovesTheTagForEveryViewerAndLeavingItBringsItBack() {
        started(Optional.of(SERVER));
        packets.send(spawnOfSuspect());
        int afterSpawn = packets.sent().size();
        director.modeChanged(suspect, SuspectMode.SPECTATING);
        assertEquals("destroy " + viewer + " [" + TAG + "]", packets.sent().get(afterSpawn));
        director.modeChanged(suspect, SuspectMode.PLAYING);
        assertTrue(packets.sent().get(afterSpawn + 2).contains("TEXT_DISPLAY"));
    }

    @Test
    void aViewResetForgetsTrackedSuspects() {
        started(Optional.of(SERVER));
        packets.send(spawnOfSuspect());
        packets.send(new ViewResetPacket(viewer, ResetCause.RESPAWN));
        EntityDestroyPacket sent = packets.send(new EntityDestroyPacket(viewer, new int[] {SUSPECT_ENTITY}));
        assertArrayEquals(new int[] {SUSPECT_ENTITY}, sent.entityNumbers());
    }

    @Test
    void ownArmourStandFollowsTheStaffMembersOwnMovesAndWaitsDuringConfiguration() {
        settings = settings.relaid(new TagLayout(0.5, OwnTag.SHOWN));
        ownPlaces.put(viewer, new SeedSighting(5, 0, 64, 0));
        started(Optional.of(new GameRelease(1, 19, 2)));
        int ownTag = TAG - 1;
        assertTrue(packets.sent().get(0).contains("entityNumber=" + ownTag) && packets.sent().get(0).contains("y=66.8"));
        int afterSpawn = packets.sent().size();
        packets.receive(new MovementPacket(viewer, 0L, 0f, 0f, false, 1, 64, 0, true, true, false));
        packets.receive(new MovementPacket(viewer, 0L, 10f, 0f, true, 0, 0, 0, false, true, false));
        List<String> after = packets.sent().subList(afterSpawn, packets.sent().size());
        assertEquals(List.of("teleport " + viewer + " " + ownTag + " 1.0 66.8 0.0", "flush " + viewer), after);
        packets.send(new ViewResetPacket(viewer, ResetCause.CONFIGURATION_START));
        int afterReset = packets.sent().size();
        director.periodic();
        director.periodic();
        assertEquals(afterReset, packets.sent().size());
        packets.send(new ViewResetPacket(viewer, ResetCause.JOIN_GAME));
        director.periodic();
        assertTrue(packets.sent().get(afterReset).contains("entityNumber=" + ownTag));
    }

    @Test
    void deafenedFollowersIgnorePackets() {
        started(Optional.of(SERVER));
        follower.deafen();
        packets.send(spawnOfSuspect());
        assertTrue(packets.sent().isEmpty());
    }

    @Override
    public Optional<HistoryView> history(UUID player) {
        return Optional.of(new HistoryView(1, List.of(new ModelResult("ultra", 0.91))));
    }

    @Override
    public boolean staff(UUID player) {
        return staff.contains(player);
    }

    @Override
    public List<SeedSighting> currentlyVisibleTo(UUID player) {
        return List.of();
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
    public void send(UUID player, List<NametagPacket> batch) {
        wire.send(player, batch);
    }
}
