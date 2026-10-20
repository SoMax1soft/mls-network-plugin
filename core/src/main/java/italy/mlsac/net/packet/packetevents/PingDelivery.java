// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

interface PingDelivery {

    void deliver(Object channel, int id);

    default void openBurst() {
    }

    default void closeBurst() {
    }
}
