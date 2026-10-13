// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.detection.DetectionSettings;
import italy.mlsac.net.config.detection.ModelRoster;
import italy.mlsac.net.config.detection.ModelSwitch;
import italy.mlsac.net.testkit.TestConfigs;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfiguredSwitchesTest {

    @Test
    void switchesFollowEveryPublishedSnapshot() {
        EffectiveConfig defaults = TestConfigs.defaults();
        ConfigReference config = new ConfigReference(defaults);
        ConfiguredSwitches switches = new ConfiguredSwitches(config, null);
        assertTrue(switches.detectionEnabled());
        assertFalse(switches.ultraEnabled());

        config.publish(withDetection(defaults, true, true));
        assertTrue(switches.ultraEnabled());

        config.publish(withDetection(defaults, false, false));
        assertFalse(switches.detectionEnabled());
        assertFalse(switches.ultraEnabled());

        config.publish(withDetection(defaults, true, false));
        assertTrue(switches.detectionEnabled());
        assertFalse(switches.ultraEnabled());

        config.publish(defaults);
        assertFalse(switches.ultraEnabled());
    }

    private static EffectiveConfig withDetection(EffectiveConfig base, boolean enabled, boolean ultra) {
        DetectionSettings detection = base.detection();
        ModelRoster models = new ModelRoster(Map.of(ModelRoster.ULTRA,
                ModelSwitch.named(ModelRoster.ULTRA, "Ultra", ultra, false)));
        return new EffectiveConfig(base.core(), new DetectionSettings(enabled, detection.regions(), models,
                detection.legacySequence(), detection.legacyStep()), base.alerts(), base.violation(), base.penalties(),
                base.alertResponses(), base.suppression(), base.modules(), base.holograms(), base.menus(),
                base.messages(), base.dataRestore(), base.dossier(), base.preset());
    }
}
