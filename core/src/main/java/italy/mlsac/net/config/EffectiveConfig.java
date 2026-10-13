// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import italy.mlsac.net.config.alert.AlertSettings;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.config.detection.DetectionSettings;
import italy.mlsac.net.config.hologram.OverheadTagConfig;
import italy.mlsac.net.config.menu.MenuLayouts;
import italy.mlsac.net.config.message.MessageSettings;
import italy.mlsac.net.config.module.ModuleSwitches;
import italy.mlsac.net.config.penalty.PenaltySettings;
import italy.mlsac.net.config.response.AlertResponseSettings;
import italy.mlsac.net.config.staff.DataRestoreSettings;
import italy.mlsac.net.config.staff.DossierSettings;
import italy.mlsac.net.config.suppression.SuppressionSettings;
import italy.mlsac.net.config.violation.ViolationSettings;
import java.util.Optional;

public record EffectiveConfig(CoreSettings core, DetectionSettings detection, AlertSettings alerts,
                              ViolationSettings violation, PenaltySettings penalties,
                              AlertResponseSettings alertResponses, Optional<SuppressionSettings> suppression,
                              ModuleSwitches modules, OverheadTagConfig holograms, MenuLayouts menus,
                              MessageSettings messages, DataRestoreSettings dataRestore, DossierSettings dossier,
                              PresetState preset) {

    public static EffectiveConfig pluginDefaults(CoreSettings core) {
        return new EffectiveConfig(core, DetectionSettings.pluginDefaults(), AlertSettings.pluginDefaults(),
                ViolationSettings.pluginDefaults(), PenaltySettings.pluginDefaults(),
                AlertResponseSettings.pluginDefaults(), Optional.empty(), ModuleSwitches.pluginDefaults(),
                OverheadTagConfig.pluginDefaults(), MenuLayouts.shipped(), MessageSettings.pluginDefaults(),
                DataRestoreSettings.pluginDefaults(), DossierSettings.pluginDefaults(), PresetState.none());
    }

    public boolean suppressionEngineActive() {
        return suppression.isPresent();
    }
}
