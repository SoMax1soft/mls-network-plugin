// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ReplayMisses {
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final Map<String, List<String>> examples = new LinkedHashMap<>();

    void note(String kind, String detail) {
        counts.merge(kind, 1, Integer::sum);
        List<String> list = examples.computeIfAbsent(kind, ignored -> new ArrayList<>());
        if (list.size() < 3) {
            list.add(detail);
        }
    }

    Map<String, Integer> counts() {
        return counts;
    }

    Map<String, List<String>> examples() {
        return examples;
    }
}
