// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import java.util.UUID;

interface ChannelAccess {

    Object channelOf(UUID player);

    boolean open(Object channel);

    boolean injected(Object channel);

    void execute(Object channel, Runnable task);
}
