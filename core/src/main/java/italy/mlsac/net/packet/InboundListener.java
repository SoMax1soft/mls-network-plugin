// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

@FunctionalInterface
public interface InboundListener<P extends InboundPacket> {

    void on(InboundContext<P> context);
}
