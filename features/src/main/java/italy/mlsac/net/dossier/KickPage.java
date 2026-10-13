// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

import java.util.List;

public record KickPage(int page, int pages, int total, boolean hasMore, List<KickEntry> entries) {

    public KickPage {
        entries = List.copyOf(entries);
    }
}
