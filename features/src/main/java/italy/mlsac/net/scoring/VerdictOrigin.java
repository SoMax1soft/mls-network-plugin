// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.function.DoubleUnaryOperator;

public enum VerdictOrigin {
    MODEL_ANSWER {
        @Override
        double count(SuspectRecord suspect, ScreenedVerdict verdict, DoubleUnaryOperator bufferUpdate) {
            return suspect.count(verdict.model(), verdict.probability(), bufferUpdate);
        }

        @Override
        public boolean visibleInGame() {
            return true;
        }
    },
    PACKET_INTEGRITY {
        @Override
        double count(SuspectRecord suspect, ScreenedVerdict verdict, DoubleUnaryOperator bufferUpdate) {
            return suspect.moveBuffer(bufferUpdate);
        }

        @Override
        public boolean visibleInGame() {
            return false;
        }
    };

    abstract double count(SuspectRecord suspect, ScreenedVerdict verdict, DoubleUnaryOperator bufferUpdate);

    public abstract boolean visibleInGame();
}
