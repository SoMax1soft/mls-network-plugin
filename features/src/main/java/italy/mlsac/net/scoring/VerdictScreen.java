// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import italy.mlsac.net.detection.choice.ModelRoute;
import italy.mlsac.net.detection.choice.PlayerModelRoutes;
import italy.mlsac.net.logbook.Logbook;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class VerdictScreen {

    private static final Set<String> ALWAYS_COUNTED = Set.of("fast", "pro");

    private final Supplier<ModelSwitchboard> models;
    private final PlayerModelRoutes routes;
    private final Logbook log;

    public VerdictScreen(Supplier<ModelSwitchboard> models, Logbook log) {
        this(models, PlayerModelRoutes.SERVER_DEFAULT, log);
    }

    public VerdictScreen(Supplier<ModelSwitchboard> models, PlayerModelRoutes routes, Logbook log) {
        this.models = models;
        this.routes = routes;
        this.log = log;
    }

    public Optional<ScreenedVerdict> screen(UUID player, String playerNick, Verdict verdict) {
        double probability = verdict.probability();
        if (!(probability >= 0.0 && probability <= 1.0)) {
            log.warn("[Detection] Dropped an answer for " + playerNick + ": probability " + probability
                    + " cannot be a probability, the payload is damaged or forged");
            return Optional.empty();
        }
        String model = verdict.model();
        if (IntegrityVerdict.concerns(model)) {
            return Optional.of(new ScreenedVerdict(IntegrityVerdict.MODEL, probability, VerdictWeight.COUNTED,
                    IntegrityVerdict.DISPLAY_NAME, VerdictOrigin.PACKET_INTEGRITY));
        }
        ModelSwitchboard switchboard = models.get();
        if (!routeAllows(routes.routeOf(player), switchboard, model)) {
            return Optional.empty();
        }
        VerdictWeight weight = switchboard.onlyAlertConfigured(model) && !alwaysCounted(model)
                ? VerdictWeight.ALERT_ONLY : VerdictWeight.COUNTED;
        return Optional.of(new ScreenedVerdict(model, probability, weight, switchboard.displayName(model)));
    }

    private static boolean routeAllows(ModelRoute route, ModelSwitchboard switchboard, String model) {
        if (route == ModelRoute.ULTRA) {
            return route.answersWith(model);
        }
        if (route.overrides()) {
            return route.answersWith(model) && switchboard.enabled(model);
        }
        return switchboard.enabled(model) && exclusiveModeAllows(switchboard, model);
    }

    private static boolean exclusiveModeAllows(ModelSwitchboard switchboard, String model) {
        boolean ultraAnswer = UltraIntake.ULTRA_MODEL.equals(normalised(model));
        return switchboard.ultraMode() == ultraAnswer;
    }

    private static boolean alwaysCounted(String model) {
        return ALWAYS_COUNTED.contains(normalised(model));
    }

    private static String normalised(String model) {
        return model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
    }
}
