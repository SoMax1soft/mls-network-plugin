// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.packet.PacketSender;
import italy.mlsac.net.pipeline.environment.TransactionPings;
import java.util.UUID;
import java.util.function.Supplier;

public final class SenderPings implements TransactionPings {

    private final Supplier<PacketSender> sender;

    public SenderPings(Supplier<PacketSender> sender) {
        this.sender = sender;
    }

    @Override
    public void send(UUID viewer, int transactionId) {
        sender.get().sendTransactionPing(viewer, transactionId);
    }

    @Override
    public void openBurst() {
        sender.get().openPingBurst();
    }

    @Override
    public void closeBurst() {
        sender.get().closePingBurst();
    }
}
