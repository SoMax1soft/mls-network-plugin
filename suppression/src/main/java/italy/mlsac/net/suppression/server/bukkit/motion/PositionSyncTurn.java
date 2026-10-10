// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import italy.mlsac.net.packet.PacketSender;
import org.bukkit.entity.Player;

public final class PositionSyncTurn implements ViewTurn {

    private final PacketSender sender;

    public PositionSyncTurn(PacketSender sender) {
        this.sender = sender;
    }

    @Override
    public void turn(Player player, int syncId, double yawDelta, double pitchDelta) {
        sender.rotateView(player.getUniqueId(), (float) yawDelta, (float) pitchDelta, syncId);
    }
}
