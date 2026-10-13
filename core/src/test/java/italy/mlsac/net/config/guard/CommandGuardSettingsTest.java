// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.ConfigAssembler;
import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.PresetLayout;
import italy.mlsac.net.config.PresetSnapshot;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.config.core.CoreSettingsReader;
import italy.mlsac.net.resource.ClasspathResources;
import italy.mlsac.net.testkit.TestConfigs;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CommandGuardSettingsTest {

    @Test
    void missingSectionMeansTheSafeDefaults() {
        assertEquals(CommandGuardSettings.pluginDefaults(), TestConfigs.core().commandGuard());
        assertTrue(CommandGuardSettings.pluginDefaults().enabled());
        assertEquals(GuardMode.BLOCK, CommandGuardSettings.pluginDefaults().mode());
    }

    @Test
    void theBundledConfigCarriesTheDefaults() {
        String bundled = new ClasspathResources(getClass().getClassLoader()).text("config.yml").orElseThrow();
        CoreSettingsReader.Result read = new CoreSettingsReader().read(TestConfigs.yaml(bundled));
        assertEquals(CommandGuardSettings.pluginDefaults(), read.settings().commandGuard());
        assertEquals(List.of(), read.warnings());
    }

    @Test
    void localValuesAreReadAndClamped() {
        CoreSettings core = TestConfigs.core("command-guard:\n  enabled: false\n  mode: WARN\n"
                + "  extra-allowed-roots: [\"/CMI\", \"essentials:mail\", \"bad root!\", \"\"]\n"
                + "  max-commands-per-minute: 0\n  max-commands-per-player-per-minute: 5\n"
                + "  max-same-command-burst: 99999\n  burst-pause-minutes: 3\n");
        CommandGuardSettings guard = core.commandGuard();
        assertFalse(guard.enabled());
        assertEquals(GuardMode.WARN, guard.mode());
        assertEquals(Set.of("cmi", "essentials:mail"), guard.extraAllowedRoots());
        assertEquals(1, guard.maxCommandsPerMinute());
        assertEquals(5, guard.maxCommandsPerPlayerPerMinute());
        assertEquals(1_000, guard.maxSameCommandBurst());
        assertEquals(3, guard.burstPauseMinutes());
    }

    @Test
    void commaSeparatedRootsAndBadValuesFallBackWithWarnings() {
        CoreSettingsReader.Result read = new CoreSettingsReader().read(TestConfigs.yaml(
                "command-guard:\n  mode: silent\n  extra-allowed-roots: \"litebans, cmi\"\n"
                        + "  max-commands-per-minute: lots\n"));
        CommandGuardSettings guard = read.settings().commandGuard();
        assertEquals(GuardMode.BLOCK, guard.mode());
        assertEquals(Set.of("litebans", "cmi"), guard.extraAllowedRoots());
        assertEquals(CommandGuardSettings.DEFAULT_MAX_PER_MINUTE, guard.maxCommandsPerMinute());
        assertEquals(2, read.warnings().size());
    }

    @Test
    void aPresetCannotTouchTheLocalSection() {
        CoreSettings local = TestConfigs.core("command-guard:\n  mode: block\n  extra-allowed-roots: [heal]\n");
        Map<String, Object> hostile = Map.of("command-guard", Map.of("enabled", false, "mode", "warn",
                "extra-allowed-roots", List.of("op", "lp")),
                "general", Map.of("command-guard", Map.of("enabled", false)));
        PresetSnapshot snapshot = new PresetSnapshot(Optional.empty(), "x", new PresetLayout().toEffective(hostile));
        EffectiveConfig config = new ConfigAssembler().assemble(local, ConfigTree.empty(), Optional.of(snapshot))
                .config();
        assertEquals(local.commandGuard(), config.core().commandGuard());
        assertTrue(config.core().commandGuard().enabled());
        assertEquals(Set.of("heal"), config.core().commandGuard().extraAllowedRoots());
    }
}
