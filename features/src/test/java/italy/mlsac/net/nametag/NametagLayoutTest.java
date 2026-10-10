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

class NametagLayoutTest implements NametagPorts {

    private static final GameRelease V1_21 = new GameRelease(1, 21, 4);
    private static final GameRelease OLD_CLIENT = new GameRelease(1, 19, 3);
    private static final List<String> TWO_LINES = List.of("&bAI:", "&f&lAVG: {AVG}% &7| {HIST}");
    private static final String AVG_LINE = "&f&lAVG: &c76% &7| &cU&f91% &cU&f97% &cU&f40%";
    private static final int VIEWER_ENTITY = 5;
    private static final int SUSPECT_ENTITY = 77;
    private static final int VIEWER_TAG = 2147483646;
    private static final int SUSPECT_TAG = 2147483645;

    private final Map<UUID, HistoryView> histories = new HashMap<>();
    private final Set<UUID> staff = new HashSet<>();
    private final Map<UUID, SeedSighting> ownPlaces = new HashMap<>();
    private final Map<UUID, List<NametagPacket>> sent = new HashMap<>();
    private final UUID viewer = UUID.nameUUIDFromBytes("Mod".getBytes());
    private final UUID suspect = UUID.nameUUIDFromBytes("Cheater".getBytes());
    private NametagSettings settings = NametagSettings.pluginDefaults();

    private NametagDirector started(Optional<GameRelease> client) {
        staff.add(viewer);
        histories.put(suspect, threeUltraResults());
        NametagDirector director = new NametagDirector(() -> settings, V1_21, new NametagText(BundledResources.hex()),
                new SuspectTags(new NametagIdentities()), this);
        director.joined(viewer, VIEWER_ENTITY, client, SuspectMode.PLAYING);
        director.joined(suspect, SUSPECT_ENTITY, Optional.empty(), SuspectMode.PLAYING);
        director.periodic();
        return director;
    }

    private NametagDirector startedWithOwnTag(Optional<GameRelease> client) {
        settings = settings.relaid(new TagLayout(0.5, OwnTag.SHOWN));
        histories.put(viewer, threeUltraResults());
        ownPlaces.put(viewer, new SeedSighting(VIEWER_ENTITY, 10, 64, 10));
        return started(client);
    }

    private static HistoryView threeUltraResults() {
        return new HistoryView(3, List.of(new ModelResult("ultra", 0.912), new ModelResult("ultra", 0.97),
                new ModelResult("ultra", 0.4)));
    }

    @Test
    void everyLineGetsItsOwnPlaceholders() {
        NametagText texts = new NametagText(BundledResources.hex());
        NametagSettings twoLines = settings.retemplated(new TagTemplates(
                List.of("&bAI: {AVG}", "&7{HIST} / {AVG}"), "&2F&f{RESULT}%", "&6P&f{RESULT}%", "&cU&f{RESULT}%",
                "&bPu&f{RESULT}%"));
        TagLines lines = texts.build(threeUltraResults().results(), twoLines).orElseThrow();
        assertEquals(List.of("&bAI: &c76", "&7&cU&f91% &cU&f97% &cU&f40% / &c76"), lines.topFirst());
        assertEquals("&bAI: &c76\n&r&7&cU&f91% &cU&f97% &cU&f40% / &c76", lines.stacked());
        assertEquals(TagLines.single(NametagText.EMPTY_LINE), texts.build(List.of(), twoLines.blankDrawn(true))
                .orElseThrow());
    }

    @Test
    void textDisplayCarriesAllLinesInOneEntity() {
        settings = settings.retemplated(templates(TWO_LINES));
        NametagDirector director = started(Optional.of(V1_21));
        List<NametagPacket> packets = sight(director).spawned(SUSPECT_ENTITY, 1, 64, 2, look());
        assertEquals(3, packets.size());
        assertEquals(SUSPECT_TAG, assertInstanceOf(NametagPacket.SpawnTextDisplay.class, packets.get(0)).entityNumber());
        NametagPacket.Metadata metadata = (NametagPacket.Metadata) packets.get(1);
        assertEquals(new MetaValue.TextValue("&bAI:\n&r" + AVG_LINE), metadata.entries().get(4).value());
        assertEquals(new NametagPacket.Passengers(SUSPECT_ENTITY, List.of(SUSPECT_TAG)), packets.get(2));
    }

    @Test
    void heightIsAddedToTheTextDisplayLift() {
        settings = settings.relaid(new TagLayout(0.5, OwnTag.HIDDEN));
        NametagDirector director = started(Optional.of(V1_21));
        NametagPacket.Metadata metadata = (NametagPacket.Metadata) sight(director)
                .spawned(SUSPECT_ENTITY, 1, 64, 2, look()).get(1);
        assertEquals(new MetaValue.Vector3fValue(0f, 0.72f + 0.5f, 0f), metadata.entries().get(0).value());
        assertEquals(-1.0, new TagLayout(-9, OwnTag.HIDDEN).extraHeight());
        assertEquals(3.0, new TagLayout(9, OwnTag.HIDDEN).extraHeight());
    }

    @Test
    void armourStandsStackUpwardsWithIdsFromTheSameCountdown() {
        settings = settings.retemplated(templates(TWO_LINES)).relaid(new TagLayout(0.5, OwnTag.HIDDEN));
        NametagDirector director = started(Optional.of(OLD_CLIENT));
        List<NametagPacket> packets = sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        assertEquals(4, packets.size());
        NametagPacket.SpawnArmorStand bottom = (NametagPacket.SpawnArmorStand) packets.get(0);
        NametagPacket.SpawnArmorStand top = (NametagPacket.SpawnArmorStand) packets.get(2);
        assertEquals(SUSPECT_TAG, bottom.entityNumber());
        assertEquals(SUSPECT_TAG - 1, top.entityNumber());
        assertEquals(64 + 2.3 + 0.5, bottom.y(), 1e-9);
        assertEquals(64 + 2.3 + 0.5 + 0.27, top.y(), 1e-9);
        assertEquals(new MetaValue.OptionalTextValue(AVG_LINE),
                ((NametagPacket.Metadata) packets.get(1)).entries().get(1).value());
        assertEquals(new MetaValue.OptionalTextValue("&bAI:"),
                ((NametagPacket.Metadata) packets.get(3)).entries().get(1).value());
        assertEquals(List.of(new NametagPacket.RelativeMove(SUSPECT_TAG, 1, 0, 0),
                new NametagPacket.RelativeMove(SUSPECT_TAG - 1, 1, 0, 0)), sight(director).movedBy(SUSPECT_ENTITY, 1, 0, 0));
        assertEquals(List.of(new NametagPacket.Teleport(SUSPECT_TAG, 1, 64 + (1.95 + 0.5), 0),
                new NametagPacket.Teleport(SUSPECT_TAG - 1, 1, 64 + (1.95 + 0.5 + 0.27), 0)),
                sight(director).posed(SUSPECT_ENTITY, SuspectPose.SNEAKING));
        assertEquals(List.of(SUSPECT_TAG, SUSPECT_TAG - 1),
                sight(director).destroyed(List.of(SUSPECT_ENTITY)).idsToAppend());
    }

    @Test
    void armourStandsAreRebuiltWhenTheLineCountChanges() {
        NametagDirector director = started(Optional.of(OLD_CLIENT));
        sight(director).spawned(SUSPECT_ENTITY, 0, 64, 0, look());
        settings = settings.retemplated(templates(TWO_LINES));
        director.periodic();
        List<NametagPacket> packets = sentTo(viewer);
        assertEquals(new NametagPacket.Destroy(List.of(SUSPECT_TAG)), packets.get(0));
        assertEquals(5, packets.size());
        assertEquals(SUSPECT_TAG - 1, ((NametagPacket.SpawnArmorStand) packets.get(3)).entityNumber());
    }

    @Test
    void olderSettingsNeverShowTheOwnTag() {
        histories.put(viewer, threeUltraResults());
        ownPlaces.put(viewer, new SeedSighting(VIEWER_ENTITY, 10, 64, 10));
        NametagDirector director = started(Optional.of(V1_21));
        director.periodic();
        assertTrue(sentTo(viewer).isEmpty());
        assertEquals(0, sight(director).trackedCount());
    }

    @Test
    void staffSeeTheirOwnTagRidingTheirOwnEntity() {
        NametagDirector director = startedWithOwnTag(Optional.of(V1_21));
        List<NametagPacket> packets = sentTo(viewer);
        assertEquals(3, packets.size());
        NametagPacket.SpawnTextDisplay spawn = assertInstanceOf(NametagPacket.SpawnTextDisplay.class, packets.get(0));
        assertEquals(VIEWER_TAG, spawn.entityNumber());
        assertEquals(65.8, spawn.y(), 1e-9);
        assertEquals(new MetaValue.Vector3fValue(0f, 0.72f + 0.5f, 0f),
                ((NametagPacket.Metadata) packets.get(1)).entries().get(0).value());
        assertEquals(new NametagPacket.Passengers(VIEWER_ENTITY, List.of(VIEWER_TAG)), packets.get(2));
        assertEquals(Optional.of(List.of(3, VIEWER_TAG)), sight(director).passengers(VIEWER_ENTITY, List.of(3)));
        assertTrue(sight(director).ownMoved(11, 64, 10).isEmpty());
    }

    @Test
    void nonStaffNeverSeeTheirOwnTag() {
        settings = settings.relaid(new TagLayout(0, OwnTag.SHOWN));
        histories.put(viewer, threeUltraResults());
        ownPlaces.put(viewer, new SeedSighting(VIEWER_ENTITY, 10, 64, 10));
        histories.put(suspect, threeUltraResults());
        NametagDirector director = new NametagDirector(() -> settings, V1_21, new NametagText(BundledResources.hex()),
                new SuspectTags(new NametagIdentities()), this);
        director.joined(viewer, VIEWER_ENTITY, Optional.of(V1_21), SuspectMode.PLAYING);
        director.periodic();
        director.periodic();
        assertTrue(sentTo(viewer).isEmpty());
    }

    @Test
    void ownTagLeavesWhenTheOptionGoesOff() {
        NametagDirector director = startedWithOwnTag(Optional.of(V1_21));
        sent.clear();
        settings = settings.relaid(new TagLayout(0.5, OwnTag.HIDDEN));
        director.periodic();
        assertEquals(List.of(new NametagPacket.Destroy(List.of(VIEWER_TAG))), sentTo(viewer));
        assertEquals(0, sight(director).trackedCount());
    }

    @Test
    void ownTagLeavesWhenStaffIsLost() {
        NametagDirector director = startedWithOwnTag(Optional.of(V1_21));
        sent.clear();
        staff.remove(viewer);
        director.periodic();
        assertEquals(List.of(new NametagPacket.Destroy(List.of(VIEWER_TAG))), sentTo(viewer));
        assertEquals(0, sight(director).trackedCount());
    }

    @Test
    void spectatorStaffHideTheirOwnTagUntilTheyLeaveSpectator() {
        NametagDirector director = startedWithOwnTag(Optional.of(V1_21));
        sent.clear();
        director.modeChanged(viewer, SuspectMode.SPECTATING);
        assertEquals(List.of(new NametagPacket.Destroy(List.of(VIEWER_TAG))), sentTo(viewer));
        sent.clear();
        director.periodic();
        assertTrue(sentTo(viewer).isEmpty());
        director.modeChanged(viewer, SuspectMode.PLAYING);
        assertInstanceOf(NametagPacket.SpawnTextDisplay.class, sentTo(viewer).get(0));
    }

    @Test
    void ownArmourStandsFollowTheStaffMember() {
        settings = settings.retemplated(templates(TWO_LINES));
        NametagDirector director = startedWithOwnTag(Optional.of(OLD_CLIENT));
        List<NametagPacket> packets = sentTo(viewer);
        assertEquals(4, packets.size());
        assertEquals(64 + 2.3 + 0.5, ((NametagPacket.SpawnArmorStand) packets.get(0)).y(), 1e-9);
        assertEquals(List.of(new NametagPacket.Teleport(VIEWER_TAG, 12, 65 + (2.3 + 0.5), 10),
                new NametagPacket.Teleport(VIEWER_TAG - 1, 12, 65 + (2.3 + 0.5 + 0.27), 10)),
                sight(director).ownMoved(12, 65, 10));
        assertTrue(sight(director).ownMoved(12, 65, 10).isEmpty());
        assertEquals(2, sight(director).posed(VIEWER_ENTITY, SuspectPose.SNEAKING).size());
    }

    @Test
    void ownTagWaitsWhileConfiguringAndReturnsAfterRespawn() {
        NametagDirector director = startedWithOwnTag(Optional.of(V1_21));
        sight(director).viewReset(ClientStage.CONFIGURING);
        sent.clear();
        director.periodic();
        assertTrue(sentTo(viewer).isEmpty());
        sight(director).viewReset(ClientStage.IN_WORLD);
        director.periodic();
        assertInstanceOf(NametagPacket.SpawnTextDisplay.class, sentTo(viewer).get(0));
    }

    @Test
    void newOwnEntityNumberMovesTheOwnTag() {
        NametagDirector director = startedWithOwnTag(Optional.of(V1_21));
        sent.clear();
        director.entityChanged(viewer, 6);
        assertEquals(List.of(new NametagPacket.Destroy(List.of(VIEWER_TAG))), sentTo(viewer));
        ownPlaces.put(viewer, new SeedSighting(6, 0, 70, 0));
        director.periodic();
        assertEquals(new NametagPacket.Passengers(6, List.of(SUSPECT_TAG - 1)), sentTo(viewer).get(3));
    }

    @Test
    void stopRemovesTheOwnTag() {
        NametagDirector director = startedWithOwnTag(Optional.of(V1_21));
        sent.clear();
        director.stop();
        assertEquals(List.of(new NametagPacket.Destroy(List.of(VIEWER_TAG))), sentTo(viewer));
    }

    private static TagTemplates templates(List<String> lines) {
        return new TagTemplates(lines, "&2F&f{RESULT}%", "&6P&f{RESULT}%", "&cU&f{RESULT}%", "&bPu&f{RESULT}%");
    }

    private ViewerSight sight(NametagDirector director) {
        return director.sight(viewer).orElseThrow();
    }

    private List<NametagPacket> sentTo(UUID player) {
        return sent.getOrDefault(player, List.of());
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
    public void send(UUID player, List<NametagPacket> packets) {
        sent.computeIfAbsent(player, key -> new ArrayList<>()).addAll(packets);
    }
}
