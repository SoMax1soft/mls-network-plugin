// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.config.response.TrickKind;
import italy.mlsac.net.config.suppression.CommandAction;
import italy.mlsac.net.config.suppression.DamageDealtAction;
import italy.mlsac.net.config.suppression.DamageTakenAction;
import italy.mlsac.net.config.suppression.LagAction;
import italy.mlsac.net.config.suppression.LagEffect;
import italy.mlsac.net.config.suppression.SuppressionAction;
import italy.mlsac.net.config.suppression.TrickAction;
import italy.mlsac.net.suppression.lag.LagKind;
import italy.mlsac.net.suppression.lag.LagLevel;
import italy.mlsac.net.suppression.plan.PlannedAction;
import italy.mlsac.net.suppression.plan.TrickType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

final class ActionTranslation {

    private final Map<Class<? extends SuppressionAction>, Function<SuppressionAction, Optional<PlannedAction>>> rules =
            Map.of(
                    CommandAction.class, action -> command((CommandAction) action),
                    DamageDealtAction.class, action -> dealt((DamageDealtAction) action),
                    DamageTakenAction.class, action -> taken((DamageTakenAction) action),
                    LagAction.class, action -> lag((LagAction) action),
                    TrickAction.class, action -> trick((TrickAction) action));

    Optional<PlannedAction> translate(SuppressionAction action) {
        Function<SuppressionAction, Optional<PlannedAction>> rule = rules.get(action.getClass());
        return rule == null ? Optional.empty() : rule.apply(action);
    }

    private static Optional<PlannedAction> command(CommandAction action) {
        return Optional.of(new italy.mlsac.net.suppression.plan.CommandAction(action.stage(), action.command(),
                action.animation(), action.animationId()));
    }

    private static Optional<PlannedAction> dealt(DamageDealtAction action) {
        return Optional.of(new italy.mlsac.net.suppression.plan.DamageDealtAction(action.stage(), action.percent(),
                action.seconds()));
    }

    private static Optional<PlannedAction> taken(DamageTakenAction action) {
        return Optional.of(new italy.mlsac.net.suppression.plan.DamageTakenAction(action.stage(), action.percent(),
                action.seconds()));
    }

    private static Optional<PlannedAction> lag(LagAction action) {
        List<LagKind> kinds = action.effects().stream().map(LagEffect::name).map(LagKind::valueOf).toList();
        return Optional.of(new italy.mlsac.net.suppression.plan.LagAction(action.stage(),
                LagLevel.valueOf(action.level().name()), kinds, action.seconds()));
    }

    private static Optional<PlannedAction> trick(TrickAction action) {
        return trickType(action.kind()).map(type -> new italy.mlsac.net.suppression.plan.TrickAction(action.stage(),
                type));
    }

    static Optional<TrickType> trickType(TrickKind kind) {
        return Arrays.stream(TrickType.values()).filter(type -> type.profile().settingId().equals(kind.id())).findFirst();
    }
}
