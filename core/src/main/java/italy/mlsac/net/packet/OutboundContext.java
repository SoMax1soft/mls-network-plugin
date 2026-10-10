// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

public interface OutboundContext<P extends OutboundPacket> {

    P packet();

    boolean cancelled();

    void cancel();

    void replace(P rewritten);

    void afterSend(Runnable action);
}
