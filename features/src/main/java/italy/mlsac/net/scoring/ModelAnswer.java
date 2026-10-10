// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.Optional;

public record ModelAnswer(double probability, String model, Optional<String> error) {

    public static ModelAnswer of(double probability, String model) {
        return new ModelAnswer(probability, model, Optional.empty());
    }
}
