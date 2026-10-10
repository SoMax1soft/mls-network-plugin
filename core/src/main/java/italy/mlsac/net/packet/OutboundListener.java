// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

@FunctionalInterface
public interface OutboundListener<P extends OutboundPacket> {

    void on(OutboundContext<P> context);
}
