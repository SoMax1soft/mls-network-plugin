// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

final class ChannelRoute {
    private volatile Object channel;

    Object channel() {
        return channel;
    }

    void channel(Object known) {
        channel = known;
    }
}
