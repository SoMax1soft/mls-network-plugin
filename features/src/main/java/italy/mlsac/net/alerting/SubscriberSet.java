// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SubscriberSet {

    private final Set<UUID> members = ConcurrentHashMap.newKeySet();

    public void add(UUID player) {
        members.add(player);
    }

    public void remove(UUID player) {
        members.remove(player);
    }

    public boolean toggle(UUID player) {
        if (members.remove(player)) {
            return false;
        }
        members.add(player);
        return true;
    }

    public boolean contains(UUID player) {
        return members.contains(player);
    }

    public List<UUID> members() {
        return List.copyOf(members);
    }

    public void clear() {
        members.clear();
    }
}
