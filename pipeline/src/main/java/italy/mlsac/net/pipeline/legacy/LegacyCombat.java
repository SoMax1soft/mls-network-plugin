// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.legacy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

final class LegacyCombat {
    static final int WINDOW = 40;
    static final int STEP = 10;
    private static final int HISTORY = 5000;

    private final LegacyRotationAnalyzer analyzer = new LegacyRotationAnalyzer();
    private final ArrayDeque<LegacyDelta> window = new ArrayDeque<>();
    private final ArrayDeque<LegacyDelta> history = new ArrayDeque<>();
    private final boolean bedrock;
    private volatile int ticksAfterHit = WINDOW + 1;
    private int stepCounter;

    LegacyCombat(boolean bedrock) {
        this.bedrock = bedrock;
    }

    boolean bedrock() {
        return bedrock;
    }

    boolean engagedInFight() {
        return ticksAfterHit <= WINDOW;
    }

    void gameTick() {
        if (ticksAfterHit <= WINDOW + 1) {
            ticksAfterHit++;
        }
        if (ticksAfterHit > WINDOW) {
            window.clear();
            stepCounter = 0;
        }
    }

    void attackedPlayer() {
        if (!engagedInFight()) {
            window.clear();
            analyzer.reset();
        }
        ticksAfterHit = 0;
    }

    List<LegacyDelta> rotate(float yaw, float pitch) {
        LegacyDelta delta = analyzer.analyze(yaw, pitch);
        window.addLast(delta);
        if (window.size() > WINDOW) {
            window.pollFirst();
        }
        history.addLast(delta);
        if (history.size() > HISTORY) {
            history.pollFirst();
        }
        stepCounter++;
        if (stepCounter >= STEP && window.size() == WINDOW) {
            stepCounter = 0;
            return new ArrayList<>(window);
        }
        return List.of();
    }

    void teleported() {
        analyzer.reset();
        window.clear();
    }

    List<LegacyDelta> history() {
        return new ArrayList<>(history);
    }
}
