// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

public interface BufferReactions {

    BufferReactions NONE = new BufferReactions() {
        @Override
        public void bufferMoved(AnswerMoment moment) {
        }

        @Override
        public void answerAccepted(AnswerMoment moment) {
        }

        @Override
        public void bufferReset(SuspectRecord suspect, double buffer) {
        }
    };

    void bufferMoved(AnswerMoment moment);

    void answerAccepted(AnswerMoment moment);

    void bufferReset(SuspectRecord suspect, double buffer);
}
