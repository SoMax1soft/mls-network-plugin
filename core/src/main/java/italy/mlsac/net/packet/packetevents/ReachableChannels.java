// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ReachableChannels {

    private final ChannelAccess access;
    private final PacketFaultLog faults;
    private static final int FIRST_PRUNE_AT = 64;

    private final Set<Object> injectedSeen = ConcurrentHashMap.newKeySet();
    private volatile int pruneAt = FIRST_PRUNE_AT;

    ReachableChannels(ChannelAccess access, PacketFaultLog faults) {
        this.access = access;
        this.faults = faults;
    }

    Object channelOrNull(UUID player) {
        Object channel = access.channelOf(player);
        return channel != null && carries(channel) ? channel : null;
    }

    Object routed(ChannelRoute route, UUID player) {
        Object known = route.channel();
        if (known != null) {
            try {
                if (access.open(known)) {
                    return known;
                }
            } catch (RuntimeException failure) {
                faults.recordNetworkFault("a channel check", failure);
                route.channel(null);
                return null;
            }
        }
        Object fresh = channelOrNull(player);
        route.channel(fresh);
        return fresh;
    }

    boolean carries(Object channel) {
        try {
            return access.open(channel) && (injectedSeen.contains(channel) || confirmInjected(channel));
        } catch (RuntimeException failure) {
            faults.recordNetworkFault("a channel check", failure);
            return false;
        }
    }

    private boolean confirmInjected(Object channel) {
        if (!access.injected(channel)) {
            return false;
        }
        injectedSeen.add(channel);
        if (injectedSeen.size() >= pruneAt) {
            pruneClosed();
        }
        return true;
    }

    private synchronized void pruneClosed() {
        if (injectedSeen.size() < pruneAt) {
            return;
        }
        injectedSeen.removeIf(this::closedOrBroken);
        pruneAt = Math.max(FIRST_PRUNE_AT, injectedSeen.size() * 2);
    }

    private boolean closedOrBroken(Object channel) {
        try {
            return !access.open(channel);
        } catch (RuntimeException failure) {
            return true;
        }
    }

    int remembered() {
        return injectedSeen.size();
    }

    boolean runOnLoop(UUID player, Runnable task) {
        Object channel = channelOrNull(player);
        return channel != null && submit(channel, "handing a task to a player's network thread", task);
    }

    boolean submit(Object channel, String what, Runnable task) {
        try {
            access.execute(channel, new ContainedTask(task, faults));
            return true;
        } catch (RuntimeException refused) {
            faults.recordNetworkFault(what, refused);
            return false;
        }
    }

    Object loopOrNull(Object channel) {
        try {
            return access.loopOf(channel);
        } catch (RuntimeException failure) {
            faults.recordNetworkFault("finding a channel's network thread", failure);
            return null;
        }
    }

    void attempt(String what, Runnable write) {
        try {
            write.run();
        } catch (RuntimeException failure) {
            faults.recordNetworkFault(what, failure);
        }
    }
}
