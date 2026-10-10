// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.PredictOutcome;
import italy.mlsac.net.backend.VerdictListener;
import italy.mlsac.net.backend.request.WindowSubmission;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class WaitingWindows {

    record Parked(WindowSubmission request, VerdictListener listener, CompletableFuture<PredictOutcome> outcome) {
    }

    private final Map<UUID, Parked> byPlayer = new HashMap<>();
    private final ArrayDeque<UUID> turns = new ArrayDeque<>();

    CompletableFuture<PredictOutcome> park(WindowSubmission request, VerdictListener listener) {
        CompletableFuture<PredictOutcome> outcome = new CompletableFuture<>();
        Parked replaced;
        synchronized (this) {
            replaced = byPlayer.put(request.player(), new Parked(request, listener, outcome));
            if (replaced == null) {
                turns.addLast(request.player());
            }
        }
        if (replaced != null) {
            replaced.outcome().complete(new PredictOutcome.Dropped());
        }
        return outcome;
    }

    synchronized Optional<Parked> next() {
        while (!turns.isEmpty()) {
            Parked parked = byPlayer.remove(turns.pollFirst());
            if (parked != null) {
                return Optional.of(parked);
            }
        }
        return Optional.empty();
    }

    synchronized boolean empty() {
        return turns.isEmpty();
    }

    void settleAll(PredictOutcome outcome) {
        List<Parked> waiting;
        synchronized (this) {
            waiting = new ArrayList<>(byPlayer.values());
            byPlayer.clear();
            turns.clear();
        }
        waiting.forEach(parked -> parked.outcome().complete(outcome));
    }
}
