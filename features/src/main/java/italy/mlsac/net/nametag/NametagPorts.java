// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import italy.mlsac.net.scoring.ModelResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NametagPorts {

    Optional<HistoryView> history(UUID suspect);

    default long historyRevision(UUID suspect) {
        return history(suspect).map(HistoryView::version).orElse(Long.MIN_VALUE);
    }

    boolean staff(UUID viewer);

    List<SeedSighting> currentlyVisibleTo(UUID viewer);

    Optional<SeedSighting> ownPlace(UUID viewer);

    void onViewerThread(UUID viewer, Runnable task);

    void send(UUID viewer, List<NametagPacket> packets);

    record HistoryView(long version, List<ModelResult> results) {

        public HistoryView {
            results = List.copyOf(results);
        }
    }
}
