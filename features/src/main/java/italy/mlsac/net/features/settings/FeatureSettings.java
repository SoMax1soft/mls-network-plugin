// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.settings;

import italy.mlsac.net.alerting.AlertOutput;
import italy.mlsac.net.menu.MenuLayouts;
import italy.mlsac.net.nametag.NametagSettings;
import italy.mlsac.net.punishment.PenaltyRules;
import italy.mlsac.net.response.ResponseRules;
import italy.mlsac.net.scoring.ModelSwitchboard;
import italy.mlsac.net.scoring.ReportingPolicy;
import italy.mlsac.net.scoring.ScoringRules;
import italy.mlsac.net.scoring.ViolationDecayRules;

public record FeatureSettings(boolean detectionEnabled, boolean engineOwnsPenalties, boolean menusOn,
                              ScoringRules scoring, ReportingPolicy reporting, ViolationDecayRules vlDecay,
                              ModelSwitchboard models, ResponseRules responses, PenaltyRules penalties,
                              AlertOutput alerts, NametagSettings nametags, MenuLayouts menus) {
}
