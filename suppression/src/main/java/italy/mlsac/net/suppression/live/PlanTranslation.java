// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.response.PrankRule;
import italy.mlsac.net.config.suppression.BufferRule;
import italy.mlsac.net.config.suppression.SuppressionAction;
import italy.mlsac.net.config.suppression.SuppressionSettings;
import italy.mlsac.net.config.suppression.SuppressionStep;
import italy.mlsac.net.suppression.plan.BufferFiring;
import italy.mlsac.net.suppression.plan.BufferRulePlan;
import italy.mlsac.net.suppression.plan.BufferRules;
import italy.mlsac.net.suppression.plan.Capacity;
import italy.mlsac.net.suppression.plan.ConsoleEcho;
import italy.mlsac.net.suppression.plan.FlagLadder;
import italy.mlsac.net.suppression.plan.InGameAction;
import italy.mlsac.net.suppression.plan.LadderRepeat;
import italy.mlsac.net.suppression.plan.LadderStep;
import italy.mlsac.net.suppression.plan.LegacyTrickEntry;
import italy.mlsac.net.suppression.plan.PlannedAction;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.plan.TrickTunings;
import java.util.List;
import java.util.Optional;

public final class PlanTranslation {

    private final ActionTranslation actions = new ActionTranslation();

    public Optional<SuppressionPlan> translate(EffectiveConfig config) {
        return config.suppression().map(settings -> plan(settings, config));
    }

    private SuppressionPlan plan(SuppressionSettings settings, EffectiveConfig config) {
        FlagLadder ladder = new FlagLadder(LadderRepeat.fromSwitch(settings.repeatLast()),
                settings.steps().stream().map(this::step).toList());
        BufferRules buffer = new BufferRules(BufferFiring.fromSwitch(settings.bufferEnabled()),
                settings.bufferRules().stream().map(this::rule).toList());
        return new SuppressionPlan(ladder, buffer, Capacity.ofThreshold(config.violation().threshold()),
                TrickTunings.fromLegacyEntries(trickEntries(config.alertResponses().prankRules())),
                ConsoleEcho.fromSwitch(config.alerts().console()));
    }

    private LadderStep step(SuppressionStep step) {
        return new LadderStep(step.flag(), planned(step.actions()));
    }

    private BufferRulePlan rule(BufferRule rule) {
        List<InGameAction> inGame = planned(rule.actions()).stream().filter(InGameAction.class::isInstance)
                .map(InGameAction.class::cast).toList();
        return new BufferRulePlan(rule.percent(), rule.enabled(), rule.restSeconds(), inGame);
    }

    private List<PlannedAction> planned(List<SuppressionAction> configured) {
        return configured.stream().map(actions::translate).flatMap(Optional::stream).toList();
    }

    private static List<LegacyTrickEntry> trickEntries(List<PrankRule> pranks) {
        return pranks.stream().flatMap(prank -> ActionTranslation.trickType(prank.kind()).stream()
                .map(type -> new LegacyTrickEntry(type, prank.bladeRequired(), prank.sidewaysPush(),
                        prank.upwardPush(), prank.message()))).toList();
    }
}
