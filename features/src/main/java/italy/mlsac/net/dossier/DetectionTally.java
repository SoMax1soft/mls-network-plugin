// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

import java.util.List;
import java.util.OptionalLong;

public record DetectionTally(int total, int day, int week, double max, double average, OptionalLong lastAt,
                             List<ModelTally> models) {

    public DetectionTally {
        models = List.copyOf(models);
    }
}
