// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.ConfigAssembler;
import italy.mlsac.net.config.ConfigIssue;
import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.IssueSeverity;
import italy.mlsac.net.config.PresetLayout;
import italy.mlsac.net.config.PresetSnapshot;
import italy.mlsac.net.config.response.TrickKind;
import italy.mlsac.net.config.violation.ViolationSettings;
import italy.mlsac.net.testkit.TestConfigs;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SuppressionSettingsTest {

    private final ConfigAssembler assembler = new ConfigAssembler();

    private static Map<String, Object> step(int flag, Map<String, Object> action) {
        return Map.of("flag", flag, "actions", List.of(action));
    }

    private static Map<String, Object> kick() {
        return Map.of("type", "command", "command", "kick {PLAYER}");
    }

    private ConfigAssembler.AssembledConfig assemble(Map<String, Object> suppression) {
        PresetSnapshot snapshot = new PresetSnapshot(Optional.empty(), "h",
                new PresetLayout().toEffective(Map.of("suppression", suppression)));
        return assembler.assemble(TestConfigs.core(), ConfigTree.empty(), Optional.of(snapshot));
    }

    private SuppressionSettings settings(Map<String, Object> suppression) {
        return assemble(suppression).config().suppression().orElseThrow();
    }

    @Test
    void repeatLastSelectsTheHighestStepBeyondTheLadder() {
        SuppressionSettings repeat = settings(Map.of("repeat-last", true,
                "steps", List.of(step(1, kick()), step(2, kick()))));
        assertEquals(List.of(1, 2, 2, 2), List.of(1, 2, 3, 4).stream()
                .map(flag -> repeat.stepForFlag(flag).orElseThrow().flag()).toList());
        SuppressionSettings once = settings(Map.of("repeat-last", false,
                "steps", List.of(step(1, kick()), step(2, kick()))));
        assertTrue(once.stepForFlag(3).isEmpty());
        assertTrue(once.stepForFlag(4).isEmpty());
    }

    @Test
    void gapsInStepNumbersRunNothing() {
        SuppressionSettings gaps = settings(Map.of("repeat-last", true, "steps", List.of(step(1, kick()), step(3, kick()))));
        assertTrue(gaps.stepForFlag(2).isEmpty());
        assertEquals(3, gaps.stepForFlag(9).orElseThrow().flag());
    }

    @Test
    void absentBufferUsesDefaultRulesAndSwitchDefaultsOff() {
        SuppressionSettings settings = settings(Map.of("steps", List.of()));
        assertFalse(settings.bufferEnabled());
        assertEquals(List.of(25, 50, 75), settings.bufferRules().stream().map(BufferRule::percent).toList());
        assertEquals(List.of(new DamageDealtAction(0, 65, 90), new DamageTakenAction(0, 75, 90)),
                settings.bufferRules().get(2).actions());
        assertTrue(settings.steps().isEmpty());
    }

    @Test
    void invalidActionsAreDroppedWithWarnings() {
        ConfigAssembler.AssembledConfig assembled = assemble(Map.of(
                "buffer-enabled", true,
                "steps", List.of(
                        Map.of("flag", 1, "actions", List.of(
                                Map.of("type", "damage_dealt", "percent", 150, "seconds", 10),
                                Map.of("type", "lag", "level", "extreme", "seconds", 5),
                                Map.of("type", "lag", "level", "heavy", "effects", List.of("rotate", "delay", "rotate"),
                                        "seconds", 5, "stage", 3),
                                Map.of("type", "teleport"))),
                        Map.of("flag", 1, "actions", List.of(kick())),
                        Map.of("flag", 2, "actions", List.of(Map.of("type", "damage_taken", "percent", 4, "seconds", 1)))),
                "buffer", List.of(
                        Map.of("percent", 30, "actions", List.of(kick(), Map.of("type", "launch"))),
                        Map.of("percent", 30, "actions", List.of(Map.of("type", "drop_weapon"))))));
        SuppressionSettings settings = assembled.config().suppression().orElseThrow();
        assertEquals(1, settings.steps().size());
        assertEquals(List.of(new LagAction(3, LagLevel.HEAVY, List.of(LagEffect.DELAY, LagEffect.ROTATE), 5)),
                settings.steps().get(0).actions());
        assertEquals(1, settings.bufferRules().size());
        assertEquals(List.of(new TrickAction(0, TrickKind.LAUNCH)),
                settings.bufferRules().get(0).actions());
        List<ConfigIssue> warnings = assembled.issues().stream()
                .filter(issue -> issue.severity() == IssueSeverity.WARNING).toList();
        assertTrue(warnings.size() >= 6, warnings.toString());
        assertTrue(warnings.stream().allMatch(issue -> issue.path().startsWith("suppression.")), warnings.toString());
    }

    @Test
    void capacityIsAtLeastThirty() {
        ViolationSettings defaults = ViolationSettings.pluginDefaults();
        assertEquals(40.0, defaults.suppressionCapacity());
        ViolationSettings low = new ViolationSettings(20.0, 10.0, 45.0, defaults.decay(), defaults.vlDecay());
        assertEquals(30.0, low.suppressionCapacity());
        assertEquals(12.5, new BufferRule(25, true, 0, List.of()).threshold(50.0));
    }
}
