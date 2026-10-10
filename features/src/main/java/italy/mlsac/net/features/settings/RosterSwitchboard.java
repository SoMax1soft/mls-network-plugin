// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.settings;

import italy.mlsac.net.config.detection.ModelRoster;
import italy.mlsac.net.scoring.ModelSwitchboard;

record RosterSwitchboard(ModelRoster roster) implements ModelSwitchboard {

    @Override
    public boolean ultraMode() {
        return roster.ultraMode();
    }

    @Override
    public boolean enabled(String model) {
        return roster.enabled(model);
    }

    @Override
    public boolean onlyAlertConfigured(String model) {
        return roster.lookup(model).onlyAlert();
    }

    @Override
    public String displayName(String model) {
        return roster.displayName(model);
    }
}
