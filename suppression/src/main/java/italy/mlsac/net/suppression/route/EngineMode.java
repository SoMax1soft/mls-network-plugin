// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.route;

import italy.mlsac.net.suppression.SuppressionEngine;
import italy.mlsac.net.suppression.trigger.BufferAnswer;
import italy.mlsac.net.suppression.trigger.FlagEvent;
import java.util.UUID;

public final class EngineMode implements ResponseMode {

    private final SuppressionEngine engine;

    public EngineMode(SuppressionEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onFlag(FlagEvent flag) {
        engine.onFlag(flag);
    }

    @Override
    public void onAnswer(BufferAnswer answer) {
        engine.onAnswer(answer);
    }

    @Override
    public void onBufferLowered(UUID suspectId, double buffer) {
        engine.onBufferLowered(suspectId, buffer);
    }

    @Override
    public void leave() {
        engine.release();
    }
}
