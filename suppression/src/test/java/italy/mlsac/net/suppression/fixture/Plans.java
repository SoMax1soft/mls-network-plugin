// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.fixture;

import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.plan.BufferFiring;
import italy.mlsac.net.suppression.plan.BufferRulePlan;
import italy.mlsac.net.suppression.plan.BufferRules;
import italy.mlsac.net.suppression.plan.Capacity;
import italy.mlsac.net.suppression.plan.CommandAction;
import italy.mlsac.net.suppression.plan.ConsoleEcho;
import italy.mlsac.net.suppression.plan.DamageDealtAction;
import italy.mlsac.net.suppression.plan.DamageTakenAction;
import italy.mlsac.net.suppression.plan.FlagLadder;
import italy.mlsac.net.suppression.plan.InGameAction;
import italy.mlsac.net.suppression.plan.LadderRepeat;
import italy.mlsac.net.suppression.plan.LadderStep;
import italy.mlsac.net.suppression.plan.LagAction;
import italy.mlsac.net.suppression.plan.PlannedAction;
import italy.mlsac.net.suppression.plan.SuppressionPlan;
import italy.mlsac.net.suppression.plan.TrickAction;
import italy.mlsac.net.suppression.plan.TrickTunings;
import italy.mlsac.net.suppression.plan.TrickType;
import java.util.List;

public final class Plans {

    public static final double THRESHOLD = 40.0;

    private Plans() {
    }

    public static SuppressionPlan ladder(LadderRepeat repeat, LadderStep... steps) {
        return new SuppressionPlan(new FlagLadder(repeat, List.of(steps)), new BufferRules(BufferFiring.ENABLED, List.of()),
                Capacity.ofThreshold(THRESHOLD), TrickTunings.defaults(), ConsoleEcho.SILENT);
    }

    public static SuppressionPlan ladder(LadderStep... steps) {
        return ladder(LadderRepeat.REPEAT_LAST, steps);
    }

    public static SuppressionPlan rules(BufferFiring firing, double threshold, BufferRulePlan... rules) {
        return new SuppressionPlan(new FlagLadder(LadderRepeat.REPEAT_LAST, List.of()),
                new BufferRules(firing, List.of(rules)), Capacity.ofThreshold(threshold), TrickTunings.defaults(),
                ConsoleEcho.SILENT);
    }

    public static SuppressionPlan withRules(SuppressionPlan plan, BufferRules rules) {
        return new SuppressionPlan(plan.ladder(), rules, plan.capacity(), plan.tricks(), plan.consoleEcho());
    }

    public static SuppressionPlan withEcho(SuppressionPlan plan, ConsoleEcho echo) {
        return new SuppressionPlan(plan.ladder(), plan.bufferRules(), plan.capacity(), plan.tricks(), echo);
    }

    public static SuppressionPlan withTricks(SuppressionPlan plan, TrickTunings tricks) {
        return new SuppressionPlan(plan.ladder(), plan.bufferRules(), plan.capacity(), tricks, plan.consoleEcho());
    }

    public static LadderStep step(int flag, PlannedAction... actions) {
        return new LadderStep(flag, List.of(actions));
    }

    public static BufferRulePlan rule(int percent, int restSeconds, InGameAction... actions) {
        return new BufferRulePlan(percent, true, restSeconds, List.of(actions));
    }

    public static BufferRulePlan disabledRule(int percent, InGameAction... actions) {
        return new BufferRulePlan(percent, false, 0, List.of(actions));
    }

    public static DamageDealtAction dealt(int percent, int seconds, int stage) {
        return new DamageDealtAction(stage, percent, seconds);
    }

    public static DamageTakenAction taken(int percent, int seconds, int stage) {
        return new DamageTakenAction(stage, percent, seconds);
    }

    public static LagAction lag(LagLevel level, int seconds, int stage, LagKind... kinds) {
        return new LagAction(stage, level, List.of(kinds), seconds);
    }

    public static TrickAction trick(TrickType type, int stage) {
        return new TrickAction(stage, type);
    }

    public static CommandAction command(String text, int stage) {
        return new CommandAction(stage, text, false);
    }

    public static CommandAction animated(String text, int stage) {
        return new CommandAction(stage, text, true);
    }
}
