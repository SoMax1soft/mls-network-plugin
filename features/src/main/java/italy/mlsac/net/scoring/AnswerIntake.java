// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.Optional;

public interface AnswerIntake {

    Optional<Verdict> admit(ModelAnswer answer);
}
