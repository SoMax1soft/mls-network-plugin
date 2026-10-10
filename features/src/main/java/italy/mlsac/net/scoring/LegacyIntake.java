// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.Optional;

public final class LegacyIntake implements AnswerIntake {

    static final String BROKEN_ORDER_CODE = "INVALID_SEQUENCE";

    private final LegacySequenceReply sequenceReply;

    public LegacyIntake(LegacySequenceReply sequenceReply) {
        this.sequenceReply = sequenceReply;
    }

    @Override
    public Optional<Verdict> admit(ModelAnswer answer) {
        Optional<String> error = answer.error();
        if (error.isPresent() && error.get().contains(BROKEN_ORDER_CODE)) {
            sequenceReply.handle(error.get());
            return Optional.empty();
        }
        return Optional.of(new Verdict(answer.model() == null ? "" : answer.model(), answer.probability()));
    }
}
