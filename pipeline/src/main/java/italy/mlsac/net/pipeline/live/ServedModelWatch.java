// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.api.FallbackReason;
import italy.mlsac.net.backend.PredictOutcome;
import italy.mlsac.net.detection.WindowKind;
import italy.mlsac.net.detection.choice.ModelRefusals;
import italy.mlsac.net.detection.choice.ModelRoute;
import italy.mlsac.net.detection.choice.PlayerModelRoutes;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServedModelWatch {

    public static final int ANSWERS_BEFORE_NOT_SERVED = 3;

    private final PlayerModelRoutes routes;
    private final ModelRefusals refusals;
    private final Map<ModelRoute, AtomicInteger> answersWithoutTheModel = new EnumMap<>(ModelRoute.class);

    public ServedModelWatch(PlayerModelRoutes routes, ModelRefusals refusals) {
        this.routes = routes;
        this.refusals = refusals;
        for (ModelRoute route : ModelRoute.values()) {
            answersWithoutTheModel.put(route, new AtomicInteger());
        }
    }

    AnswerWatch open(UUID player, WindowKind kind) {
        ModelRoute route = routes.routeOf(player);
        if (!route.overrides()) {
            return AnswerWatch.IGNORED;
        }
        if (kind == WindowKind.ULTRA) {
            return route == ModelRoute.ULTRA ? new UltraAnswer() : AnswerWatch.IGNORED;
        }
        return route == ModelRoute.ULTRA ? AnswerWatch.IGNORED : new LegacyAnswer(route);
    }

    private boolean declined(PredictOutcome outcome, ModelRoute route) {
        if (outcome instanceof PredictOutcome.Failed failed && failed.declinedByBackend()) {
            refusals.refused(route.choice(), FallbackReason.REFUSED_BY_BACKEND);
            return true;
        }
        return false;
    }

    private final class UltraAnswer implements AnswerWatch {

        @Override
        public void verdict(String model) {
        }

        @Override
        public void settled(PredictOutcome outcome) {
            declined(outcome, ModelRoute.ULTRA);
        }
    }

    private final class LegacyAnswer implements AnswerWatch {

        private final ModelRoute route;
        private volatile boolean served;

        private LegacyAnswer(ModelRoute route) {
            this.route = route;
        }

        @Override
        public void verdict(String model) {
            if (route.answersWith(model)) {
                served = true;
            }
        }

        @Override
        public void settled(PredictOutcome outcome) {
            if (declined(outcome, route) || !(outcome instanceof PredictOutcome.Completed)) {
                return;
            }
            AtomicInteger misses = answersWithoutTheModel.get(route);
            if (served) {
                misses.set(0);
            } else if (misses.incrementAndGet() >= ANSWERS_BEFORE_NOT_SERVED) {
                misses.set(0);
                refusals.refused(route.choice(), FallbackReason.NOT_SERVED_BY_BACKEND);
            }
        }
    }
}
