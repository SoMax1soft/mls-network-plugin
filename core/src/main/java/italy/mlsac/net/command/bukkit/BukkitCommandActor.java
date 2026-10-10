// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.command.bukkit;

import italy.mlsac.net.command.CommandActor;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

record BukkitCommandActor(CommandSender sender) implements CommandActor {

    @Override
    public String name() {
        return sender.getName();
    }

    @Override
    public Optional<UUID> actorUuid() {
        return Optional.of(sender).filter(Player.class::isInstance).map(Player.class::cast).map(Player::getUniqueId);
    }

    @Override
    public boolean hasPermission(String node) {
        return sender.hasPermission(node);
    }

    @Override
    public boolean op() {
        return sender.isOp();
    }

    @Override
    public void send(String coloredText) {
        sender.sendMessage(coloredText);
    }
}
