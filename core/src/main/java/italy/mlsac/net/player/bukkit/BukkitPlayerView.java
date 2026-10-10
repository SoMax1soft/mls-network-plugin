// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player.bukkit;

import italy.mlsac.net.player.PlayerView;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

record BukkitPlayerView(Player handle) implements PlayerView {

    @Override
    public UUID id() {
        return handle.getUniqueId();
    }

    @Override
    public String name() {
        return handle.getName();
    }

    @Override
    public boolean online() {
        return handle.isOnline();
    }

    @Override
    public boolean hasPermission(String node) {
        return handle.hasPermission(node);
    }

    @Override
    public boolean op() {
        return handle.isOp();
    }

    @Override
    public void sendMessage(String coloredText) {
        handle.sendMessage(coloredText);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void sendActionBar(String coloredText) {
        handle.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(coloredText));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void kick(String coloredReason) {
        handle.kickPlayer(coloredReason);
    }
}
