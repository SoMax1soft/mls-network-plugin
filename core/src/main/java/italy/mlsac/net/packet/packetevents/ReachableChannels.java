// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

final class ReachableChannels {

    private final ChannelAccess access;
    private final PacketFaultLog faults;

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
            return access.open(channel) && access.injected(channel);
        } catch (RuntimeException failure) {
            faults.recordNetworkFault("a channel check", failure);
            return false;
        }
    }

    boolean runOnLoop(UUID player, Runnable task) {
        Object channel = channelOrNull(player);
        if (channel == null) {
            return false;
        }
        try {
            access.execute(channel, new ContainedTask(task, faults));
            return true;
        } catch (RejectedExecutionException refused) {
            faults.recordNetworkFault("handing a task to a player's network thread", refused);
            return false;
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
