// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

final class ReachableChannels {

    private final ChannelAccess access;
    private final PacketFaultLog faults;
    private final Set<Object> injectedSeen =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    ReachableChannels(ChannelAccess access, PacketFaultLog faults) {
        this.access = access;
        this.faults = faults;
    }

    Object channelOrNull(UUID player) {
        Object channel = access.channelOf(player);
        return channel != null && carries(channel) ? channel : null;
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
        return true;
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
