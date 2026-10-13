// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression;

import static italy.mlsac.net.suppression.fixture.Harness.STEVE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.suppression.fixture.BuiltInTexts;
import italy.mlsac.net.suppression.fixture.FakeBody;
import italy.mlsac.net.suppression.fixture.FakeServer;
import italy.mlsac.net.suppression.fixture.ManualClock;
import italy.mlsac.net.suppression.fixture.Timeline;
import italy.mlsac.net.suppression.motion.Vec3;
import italy.mlsac.net.suppression.notice.StockTrickTexts;
import italy.mlsac.net.suppression.notice.TrickBroadcast;
import italy.mlsac.net.suppression.notice.TrickTextChoice;
import italy.mlsac.net.suppression.plan.LegacyTrickEntry;
import italy.mlsac.net.suppression.plan.TrickTuning;
import italy.mlsac.net.suppression.plan.TrickTunings;
import italy.mlsac.net.suppression.plan.TrickType;
import italy.mlsac.net.suppression.trick.DropWeaponTrick;
import italy.mlsac.net.suppression.trick.LaunchTrick;
import italy.mlsac.net.suppression.trick.ShuffleInventoryTrick;
import italy.mlsac.net.suppression.trick.StoragePermutation;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrickTest {

    private final ManualClock clock = new ManualClock();
    private final FakeServer server = new FakeServer(new Timeline(clock));
    private final FakeBody body = server.body(STEVE);

    private static TriggerContext context(double buffer, double probability) {
        return new TriggerContext(STEVE, "Steve", 3, probability, buffer);
    }

    private TrickBroadcast broadcast() {
        return new TrickBroadcast(new TrickTextChoice(server, StockTrickTexts.legacyEditorTexts()), server);
    }

    @Test
    void t1ShufflePermutesOnlyTheStorageSlots() {
        Random seeded = new Random(42);
        String[] before = body.storageCopy();
        String[] armour = body.armourCopy();
        assertTrue(new ShuffleInventoryTrick(new StoragePermutation(seeded::nextDouble))
                .perform(body, TrickTuning.defaultsFor(TrickType.SHUFFLE_INVENTORY)));
        String[] after = body.storageCopy();
        assertFalse(Arrays.equals(before, after));
        String[] sortedBefore = before.clone();
        String[] sortedAfter = after.clone();
        Arrays.sort(sortedBefore);
        Arrays.sort(sortedAfter);
        assertArrayEquals(sortedBefore, sortedAfter);
        assertArrayEquals(armour, body.armourCopy());
        assertEquals("SHIELD", body.offHand());
        assertEquals("CURSOR_ITEM", body.cursor());
        assertEquals(3, body.selectedSlot());
    }

    @Test
    void permutationIsAUniformFisherYatesOverAllSlots() {
        int[] order = new StoragePermutation(() -> 0.0).draw(36);
        List<Integer> seen = new ArrayList<>();
        for (int slot : order) {
            seen.add(slot);
        }
        assertEquals(36, seen.stream().distinct().count());
        assertNotEquals(0, order[0]);
    }

    @Test
    void shuffleIsSkippedForDeadOrCreativeSuspects() {
        ShuffleInventoryTrick shuffle = new ShuffleInventoryTrick(new StoragePermutation(() -> 0.3));
        assertFalse(shuffle.perform(body.dead(true), TrickTuning.defaultsFor(TrickType.SHUFFLE_INVENTORY)));
        assertFalse(shuffle.perform(body.dead(false).creative(true),
                TrickTuning.defaultsFor(TrickType.SHUFFLE_INVENTORY)));
    }

    @Test
    void t2DropWeaponOnlyDropsSwordsWhenOnlySword() {
        DropWeaponTrick drop = new DropWeaponTrick();
        TrickTuning swordsOnly = TrickTuning.defaultsFor(TrickType.DROP_WEAPON);
        assertFalse(drop.perform(body.holding("DIAMOND_AXE"), swordsOnly));
        assertTrue(body.drops().isEmpty());
        assertFalse(drop.perform(body.holding(null), swordsOnly));
        assertTrue(drop.perform(body.holding("NETHERITE_SWORD").looking(new Vec3(0, 0, 2)), swordsOnly));
        assertEquals(List.of(40), body.pickupDelays());
        Vec3 velocity = body.drops().get(0);
        assertEquals(0.0, velocity.x(), 1e-9);
        assertEquals(0.55, velocity.y(), 1e-9);
        assertEquals(1.9, velocity.z(), 1e-9);
        assertEquals(Optional.empty(), body.mainHand());
        TrickTuning anyItem = new TrickTuning(false, 1.9, 0.55, "");
        assertTrue(drop.perform(body.holding("DIAMOND_AXE"), anyItem));
    }

    @Test
    void t3LaunchIsSkippedWhileGlidingOrRiding() {
        List<UUID> launched = new ArrayList<>();
        LaunchTrick launch = new LaunchTrick(launched::add);
        TrickTuning tuning = TrickTuning.defaultsFor(TrickType.LAUNCH);
        assertFalse(launch.perform(body.gliding(true), tuning));
        assertFalse(launch.perform(body.gliding(false).mounted(true), tuning));
        assertTrue(launched.isEmpty());
        assertTrue(launch.perform(body.mounted(false).looking(new Vec3(1, 0, 0)), tuning));
        Vec3 velocity = body.velocities().get(0);
        assertEquals(-1.2, velocity.x(), 1e-9);
        assertEquals(0.6, velocity.y(), 1e-9);
        assertEquals(List.of(STEVE), launched);
    }

    @Test
    void t4TextResolutionOrder() {
        TrickBroadcast broadcast = broadcast();
        TriggerContext context = context(21.04, 0.9731);
        TrickTuning stock = new TrickTuning(true, 1.2, 0.6, "&c{PLAYER} &7was launched at buffer &f{BUFFER}&7.");
        assertEquals(Optional.of(BuiltInTexts.LAUNCH.replace("{PLAYER}", "Steve").replace("{BUFFER}", "21.0")),
                broadcast.compose(TrickType.LAUNCH, stock, context));
        TrickTuning custom = new TrickTuning(true, 1.2, 0.6, "custom {PLAYER}");
        assertEquals(Optional.of("custom Steve"), broadcast.compose(TrickType.LAUNCH, custom, context));
        server.override("suppression-launch", "X");
        assertEquals(Optional.of("X"), broadcast.compose(TrickType.LAUNCH, custom, context));
        server.override("suppression-launch", "");
        assertEquals(Optional.empty(), broadcast.compose(TrickType.LAUNCH, custom, context));
        broadcast.announce(TrickType.LAUNCH, custom, context);
        assertTrue(server.staffLines().isEmpty());
    }

    @Test
    void t4bPresetAndBundledTextsTakeThePrefixButLegacyTextsDoNot() {
        server.prefix("&bGrim &8» &r{PLAYER} ");
        TriggerContext context = context(21.04, 0.9731);
        TrickTuning stock = new TrickTuning(true, 1.2, 0.6, "");
        assertEquals(Optional.of("&bGrim &8» &r{PLAYER} " + BuiltInTexts.LAUNCH.replace("{PLAYER}", "Steve")
                .replace("{BUFFER}", "21.0")), broadcast().compose(TrickType.LAUNCH, stock, context));
        TrickTuning custom = new TrickTuning(true, 1.2, 0.6, "custom {PLAYER}");
        assertEquals(Optional.of("custom Steve"), broadcast().compose(TrickType.LAUNCH, custom, context));
        server.override("suppression-launch", "{PLAYER} up");
        assertEquals(Optional.of("&bGrim &8» &r{PLAYER} Steve up"),
                broadcast().compose(TrickType.LAUNCH, custom, context));
    }

    @Test
    void t5PlaceholdersUseFixedDecimals() {
        TrickTuning tuning = new TrickTuning(true, 1.2, 0.6, "{BUFFER}|{DETECTIONS}|{PROBABILITY}|{VL}");
        assertEquals(Optional.of("21.0|21.0|0.97|3"),
                broadcast().compose(TrickType.LAUNCH, tuning, context(21.04, 0.9731)));
    }

    @Test
    void t7TrickParametersComeFromTheFirstLegacyEntry() {
        TrickTunings tunings = TrickTunings.fromLegacyEntries(List.of(
                new LegacyTrickEntry(TrickType.LAUNCH, false, 2.0, 0.3, ""),
                new LegacyTrickEntry(TrickType.LAUNCH, false, 5.0, 5.0, "")));
        assertEquals(2.0, tunings.tuningOf(TrickType.LAUNCH).sidewaysPush());
        assertEquals(0.3, tunings.tuningOf(TrickType.LAUNCH).upwardPush());
        TrickTunings none = TrickTunings.defaults();
        assertEquals(1.2, none.tuningOf(TrickType.LAUNCH).sidewaysPush());
        assertEquals(0.6, none.tuningOf(TrickType.LAUNCH).upwardPush());
        assertTrue(none.tuningOf(TrickType.DROP_WEAPON).bladeRequired());
        assertEquals(10.0, new TrickTuning(true, 99, -3, null).sidewaysPush());
        assertEquals(0.0, new TrickTuning(true, 99, -3, null).upwardPush());
    }
}
