// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.backend.PredictOutcome;

interface AnswerWatch {

    AnswerWatch IGNORED = new AnswerWatch() {
        @Override
        public void verdict(String model) {
        }

        @Override
        public void settled(PredictOutcome outcome) {
        }
    };

    void verdict(String model);

    void settled(PredictOutcome outcome);
}
