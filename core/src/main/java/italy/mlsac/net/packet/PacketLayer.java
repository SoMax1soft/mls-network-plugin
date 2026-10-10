// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import italy.mlsac.net.platform.GameVersion;

public interface PacketLayer {

    <P extends InboundPacket> Registration onInbound(Class<P> type, ListenerOrder order, InboundListener<P> listener);

    <P extends OutboundPacket> Registration onOutbound(Class<P> type, ListenerOrder order,
                                                       OutboundListener<P> listener);

    PacketSender sender();

    ClientVersions clients();

    GameVersion serverVersion();
}
