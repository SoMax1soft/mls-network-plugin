// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.detection.ModelVerdict;
import italy.mlsac.net.detection.WindowKind;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.module.PipelinePorts;
import italy.mlsac.net.detection.AimProfiles;
import italy.mlsac.net.detection.CombatStatus;
import italy.mlsac.net.testkit.FakePlayer;
import italy.mlsac.net.detection.TickRestore;
import italy.mlsac.net.testkit.TestCore;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.detection.DetectionSettings;
import italy.mlsac.net.config.detection.ModelRoster;
import italy.mlsac.net.config.detection.ModelSwitch;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeaturesWiringTest {

    @Test
    void ultraVerdictsOnTheBusRaiseTheViolationLevel() {
        TestCore core = new TestCore();
        EffectiveConfig defaults = core.config.get();
        Map<String, ModelSwitch> models = new LinkedHashMap<>(defaults.detection().models().models());
        models.put(ModelRoster.ULTRA, ModelSwitch.named(ModelRoster.ULTRA, "Ultra", true, false));
        DetectionSettings detection = defaults.detection();
        core.config.publish(new EffectiveConfig(defaults.core(), new DetectionSettings(detection.enabled(),
                detection.regions(), new ModelRoster(models), detection.legacySequence(), detection.legacyStep()),
                defaults.alerts(), defaults.violation(), defaults.penalties(), defaults.alertResponses(),
                defaults.suppression(), defaults.modules(), defaults.holograms(), defaults.menus(), defaults.messages(),
                defaults.dataRestore(), defaults.dossier(),
                defaults.preset()));
        FeaturesAssembly features = new FeaturesAssembly(core.services(),
                new PipelinePorts(TickRestore.NONE, AimProfiles.NONE, CombatStatus.NONE));
        features.start();
        FakePlayer suspect = core.players.add("Suspect");
        UUID id = suspect.id();
        core.bus.publish(new ServerTick(1));
        for (int answer = 0; answer < 40; answer++) {
            core.bus.publish(new ModelVerdict(id, "Suspect", "ultra", 0.99, WindowKind.ULTRA, answer));
            core.scheduler.advance(1);
        }
        assertTrue(features.ports().detections().offenceLevel(id) > 0);
        assertTrue(features.ports().detections().buffer(id).isPresent());
        features.stop();
        assertEquals("features", features.name());
    }
}
