// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.scoring.ModelResult;
import italy.mlsac.net.testkit.BundledResources;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NametagTextTest {

    private final NametagText texts = new NametagText(BundledResources.hex());
    private final NametagSettings defaults = NametagSettings.pluginDefaults();

    @Test
    void specificationExample() {
        List<ModelResult> history = List.of(new ModelResult("ultra", 0.912), new ModelResult("ultra", 0.97),
                new ModelResult("ultra", 0.4));
        assertEquals(Optional.of(TagLines.single("&f&lAVG: &c76% &7| &cU&f91% &cU&f97% &cU&f40%")), texts.build(history, defaults));
    }

    @Test
    void historyLimitAndModelFamilies() {
        List<ModelResult> history = List.of(new ModelResult("fast", 0.1), new ModelResult("pulse", 0.5),
                new ModelResult("pro", 0.55), new ModelResult("mini", 0.6), new ModelResult("experimental", 0.7),
                new ModelResult("ultra", 0.95));
        String line = texts.build(history, defaults).orElseThrow().stacked();
        assertEquals("&f&lAVG: &c66% &7| &bPu&f50% &6P&f55% &2F&f60% ? &cU&f95%", line);
    }

    @Test
    void emptyHistory() {
        assertEquals(Optional.empty(), texts.build(List.of(), defaults));
        NametagSettings drawWhenBlank = defaults.blankDrawn(true);
        assertEquals(Optional.of(TagLines.single(NametagText.EMPTY_LINE)), texts.build(List.of(), drawWhenBlank));
        NametagSettings disabled = defaults.blankDrawn(true).switchedOn(false);
        assertEquals(Optional.empty(), texts.build(List.of(new ModelResult("ultra", 1)), disabled));
    }

    @Test
    void bandsAndBackground() {
        List<MeanShadeThreshold> bands = MeanShadeThreshold.strictlyRising(List.of(new MeanShadeThreshold(0, "f", false), new MeanShadeThreshold(60, "c", false),
                new MeanShadeThreshold(50, "e", false)));
        assertEquals(2, bands.size());
        MeanShadeThreshold top = MeanShadeThreshold.forValue(MeanShadeThreshold.defaults(), 95);
        assertEquals("4", top.color());
        assertTrue(top.bold());
        assertEquals("&#112233", MeanShadeThreshold.forValue(List.of(new MeanShadeThreshold(10, "#112233", false)), 3).prefix());
        assertEquals(MeanShadeThreshold.defaults(), MeanShadeThreshold.strictlyRising(List.of()));
        assertEquals(0xFF000000, new NametagDisplay(true, 0, 100, true, false, 1, Billboard.CENTER).plateColour());
        assertEquals(0x40123456, new NametagDisplay(true, 0x123456, 25, true, false, 1, Billboard.CENTER).plateColour());
        assertEquals(0, new NametagDisplay(false, 0x123456, 25, true, false, 1, Billboard.CENTER).plateColour());
        assertEquals(3, new NametagDisplay(true, 0, 0, true, true, 1, Billboard.CENTER).flags());
    }

    @Test
    void hexInFormatIsNormalised() {
        NametagSettings hex = defaults.retemplated(new TagTemplates(List.of("<#FF0000>{AVG}"), "", "", "", ""));
        assertEquals("&#FF0000&f50", texts.build(List.of(new ModelResult("x", 0.5)), hex).orElseThrow().stacked());
    }

    @Test
    void identitiesCountDownWithFixedUuidPrefix() {
        NametagIdentities identities = new NametagIdentities();
        NametagIdentity first = identities.allocate();
        NametagIdentity second = identities.allocate();
        assertEquals(2147483646, first.entityNumber());
        assertEquals(2147483645, second.entityNumber());
        assertEquals(new UUID(NametagIdentity.UUID_HIGH_BITS, 2147483646L), first.uuid());
    }

    @Test
    void followRadius() {
        assertEquals(46, TrackingRange.fromSpigotSetting(OptionalInt.empty()).blocks());
        assertEquals(8, TrackingRange.fromSpigotSetting(OptionalInt.of(5)).blocks());
        assertEquals(46, TrackingRange.whenUnreadable().blocks());
        assertTrue(TrackingRange.fromSpigotSetting(OptionalInt.of(12)).covers(6, 8));
    }

    @Test
    void releaseParsing() {
        assertEquals(new GameRelease(1, 16, 5), GameRelease.parse("1.16.5-R0.1-SNAPSHOT").orElseThrow());
        assertEquals(new GameRelease(1, 21, 4), GameRelease.parse("V_1_21_4").orElseThrow());
        assertEquals(new GameRelease(1, 17, 0), GameRelease.parse("1.17").orElseThrow());
        assertTrue(GameRelease.parse("nope").isEmpty());
    }

    @Test
    void poses() {
        assertEquals(SuspectPose.SNEAKING, SuspectPose.fromGamePose("CROUCHING"));
        assertEquals(SuspectPose.LOW, SuspectPose.fromGamePose("FALL_FLYING"));
        assertEquals(SuspectPose.LOW, SuspectPose.fromGamePose("spin_attack"));
        assertEquals(SuspectPose.SLEEPING, SuspectPose.fromGamePose("SLEEPING"));
        assertEquals(SuspectPose.STANDING, SuspectPose.fromGamePose("DYING"));
        assertEquals(SuspectMode.SPECTATING, SuspectMode.fromGameMode("spectator"));
        assertEquals(SuspectMode.PLAYING, SuspectMode.fromGameMode("CREATIVE"));
    }
}
