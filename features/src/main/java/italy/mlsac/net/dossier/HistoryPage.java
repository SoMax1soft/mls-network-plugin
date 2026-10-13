// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

import java.util.List;

public record HistoryPage(String player, int page, int pages, int total, boolean hasMore,
                          List<HistoryEntry> entries) {

    public HistoryPage {
        entries = List.copyOf(entries);
    }
}
