// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

final class DirectPings implements PingDelivery {

    private final PingWrite write;

    DirectPings(PingWrite write) {
        this.write = write;
    }

    @Override
    public void deliver(Object channel, int id) {
        write.write(channel, id);
    }
}
