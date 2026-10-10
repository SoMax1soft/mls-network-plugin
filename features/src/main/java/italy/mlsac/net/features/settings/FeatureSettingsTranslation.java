// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.settings;

import italy.mlsac.net.alerting.AlertOutput;
import italy.mlsac.net.alerting.AlertSound;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.response.AlertResponseSettings;
import italy.mlsac.net.config.violation.ViolationSettings;
import italy.mlsac.net.punishment.LadderRungs;
import italy.mlsac.net.punishment.PenaltyRules;
import italy.mlsac.net.response.ReductionStage;
import italy.mlsac.net.response.ResponseRules;
import italy.mlsac.net.response.TrollTrick;
import italy.mlsac.net.scoring.ReportingPolicy;
import italy.mlsac.net.scoring.ScoringRules;
import italy.mlsac.net.scoring.ViolationDecayRules;

public final class FeatureSettingsTranslation {

    private final NametagTranslation nametags = new NametagTranslation();
    private final MenuTranslation menus = new MenuTranslation();

    public FeatureSettings translate(EffectiveConfig config) {
        ViolationSettings violation = config.violation();
        AlertResponseSettings responses = config.alertResponses();
        ScoringRules scoring = new ScoringRules(config.alerts().threshold(), violation.multiplier(),
                violation.decay().amount(), violation.decay().threshold(), violation.threshold(),
                violation.resetValue(), responses.stepPercent());
        ReportingPolicy reporting = new ReportingPolicy(config.core().identity().eventsEnabled(),
                config.core().identity().alertEventThreshold());
        ViolationDecayRules decay = new ViolationDecayRules(violation.vlDecay().enabled(),
                violation.vlDecay().everySeconds(), violation.vlDecay().amount());
        PenaltyRules penalties = new PenaltyRules(LadderRungs.of(config.penalties().ladder().rungs()),
                config.penalties().banShowOn(), config.penalties().banShowStyle(), config.penalties().nickGuard(),
                config.alerts().console());
        AlertOutput alerts = new AlertOutput(config.alerts().console(), new AlertSound(
                config.alerts().sound().enabled(), config.alerts().sound().type(), config.alerts().sound().volume(),
                config.alerts().sound().pitch()));
        return new FeatureSettings(config.detection().enabled(), config.suppressionEngineActive(),
                config.modules().menus(), scoring, reporting, decay,
                new RosterSwitchboard(config.detection().models()), responses(responses), penalties, alerts,
                nametags.translate(config.holograms()), menus.translate(config.menus()));
    }

    private static ResponseRules responses(AlertResponseSettings settings) {
        return new ResponseRules(settings.enabled(), settings.damageCutOn(),
                settings.damageStages().stream().map(step -> new ReductionStage(step.buffer(), step.cutPercent(),
                        step.lastsSeconds(), step.enabled())).toList(),
                settings.pranksOn(),
                settings.prankRules().stream().map(rule -> new TrollTrick(rule.kind().id(), rule.buffer(),
                        rule.enabled(), rule.restSeconds(), rule.bladeRequired(), rule.sidewaysPush(),
                        rule.upwardPush(), rule.message())).toList());
    }
}
