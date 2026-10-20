// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

final class LoopPings implements PingDelivery {

    private final ReachableChannels channels;
    private final PingWrite write;

    LoopPings(ReachableChannels channels, PingWrite write) {
        this.channels = channels;
        this.write = write;
    }

    @Override
    public void deliver(Object channel, int id) {
        channels.submit(channel, "handing a transaction ping to a player's network thread",
                () -> write.write(channel, id));
    }
}
