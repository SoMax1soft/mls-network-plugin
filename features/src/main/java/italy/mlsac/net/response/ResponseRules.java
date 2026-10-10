// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.response;

import java.util.Comparator;
import java.util.List;

public record ResponseRules(boolean enabled, boolean damageEnabled, List<ReductionStage> stages, boolean pranksOn,
                            List<TrollTrick> tricks) {

    public ResponseRules {
        stages = stages.stream().sorted(Comparator.comparingDouble(ReductionStage::buffer)).toList();
        tricks = tricks.stream().sorted(Comparator.comparingDouble(TrollTrick::buffer)).toList();
    }

    public static ResponseRules pluginDefaults() {
        return new ResponseRules(true, true, List.of(
                new ReductionStage(10.0, 5, 8, true),
                new ReductionStage(20.0, 35, 12, true),
                new ReductionStage(30.0, 70, 16, true)), false, List.of());
    }

    public static List<ReductionStage> fallbackStagesWhenKeyAbsent() {
        return List.of(new ReductionStage(15.0, 15, 8, true),
                new ReductionStage(30.0, 35, 12, true),
                new ReductionStage(45.0, 55, 16, true));
    }
}
