// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public final class ServerPlayerSlot {
    private final Player player;
    private AttributeInstance interactionRange;

    public ServerPlayerSlot(Player player) {
        this.player = player;
    }

    public Player player() {
        return player;
    }

    AttributeInstance interactionRange() {
        return interactionRange;
    }

    void interactionRange(AttributeInstance instance) {
        interactionRange = instance;
    }
}
