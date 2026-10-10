// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.chain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChainBoard {

    public static final int MAX_CHAINS_PER_SUSPECT = 4;

    private final Map<UUID, Deque<LadderChain>> chains = new LinkedHashMap<>();

    public synchronized List<DueStage> start(LadderChain chain, long now) {
        List<DueStage> due = chain.takeDue(now);
        if (chain.finished()) {
            return due;
        }
        Deque<LadderChain> own = chains.computeIfAbsent(chain.context().suspectId(), id -> new ArrayDeque<>());
        own.addLast(chain);
        while (own.size() > MAX_CHAINS_PER_SUSPECT) {
            own.removeFirst().dropPending();
        }
        return due;
    }

    public synchronized List<DueStage> due(long now) {
        List<DueStage> due = new ArrayList<>();
        Iterator<Map.Entry<UUID, Deque<LadderChain>>> entries = chains.entrySet().iterator();
        while (entries.hasNext()) {
            Deque<LadderChain> own = entries.next().getValue();
            for (LadderChain chain : own) {
                due.addAll(chain.takeDue(now));
            }
            own.removeIf(LadderChain::finished);
            if (own.isEmpty()) {
                entries.remove();
            }
        }
        return due;
    }

    public synchronized int unfinishedFor(UUID suspectId) {
        Deque<LadderChain> own = chains.get(suspectId);
        return own == null ? 0 : own.size();
    }

    public synchronized int pendingCommands() {
        int count = 0;
        for (Deque<LadderChain> own : chains.values()) {
            for (LadderChain chain : own) {
                count += chain.pendingCommands();
            }
        }
        return count;
    }

    public synchronized int pendingCommandsFor(UUID suspectId) {
        Deque<LadderChain> own = chains.get(suspectId);
        int count = 0;
        if (own != null) {
            for (LadderChain chain : own) {
                count += chain.pendingCommands();
            }
        }
        return count;
    }

    public synchronized void clear() {
        chains.values().forEach(own -> own.forEach(LadderChain::dropPending));
        chains.clear();
    }
}
