// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.Optional;

public final class UltraIntake implements AnswerIntake {

    public static final String ULTRA_MODEL = "ultra";

    @Override
    public Optional<Verdict> admit(ModelAnswer answer) {
        if (answer.error().isPresent() || !Double.isFinite(answer.probability())) {
            return Optional.empty();
        }
        return Optional.of(new Verdict(ULTRA_MODEL, answer.probability()));
    }
}
