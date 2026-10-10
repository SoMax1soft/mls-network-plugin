// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.List;

public record ModelHistory(long revision, List<ModelResult> results) {

    public ModelHistory {
        results = List.copyOf(results);
    }
}
