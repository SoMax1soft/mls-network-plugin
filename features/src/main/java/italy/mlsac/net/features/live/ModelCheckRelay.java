// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.detection.choice.LegacyCheckObserved;
import italy.mlsac.net.detection.choice.ModelRoute;
import italy.mlsac.net.detection.choice.UltraFlagObserved;
import italy.mlsac.net.scoring.CheckObserver;
import italy.mlsac.net.scoring.CheckOutcome;
import italy.mlsac.net.scoring.ScreenedVerdict;
import italy.mlsac.net.scoring.VerdictOrigin;
import java.util.Optional;

public final class ModelCheckRelay implements CheckObserver {

    private final EventBus bus;

    public ModelCheckRelay(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public void checked(CheckOutcome outcome) {
        ScreenedVerdict verdict = outcome.verdict();
        if (verdict.origin() != VerdictOrigin.MODEL_ANSWER) {
            return;
        }
        Optional<ModelChoice> model = ModelRoute.answeringChoice(verdict.model());
        if (model.isEmpty()) {
            return;
        }
        if (model.get() == ModelChoice.ULTRA) {
            if (outcome.flagged()) {
                bus.publish(new UltraFlagObserved(outcome.player(), outcome.playerNick()));
            }
            return;
        }
        bus.publish(new LegacyCheckObserved(outcome.player(), outcome.playerNick(), model.get(),
                verdict.probability(), outcome.buffer(), outcome.flagged()));
    }
}
