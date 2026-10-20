// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

@FunctionalInterface
interface PingWrite {

    void write(Object channel, int id);
}
