// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

public interface InboundContext<P extends InboundPacket> {

    P packet();

    boolean cancelled();

    void cancel();

    HeldPacket hold();
}
