/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package wtf.mlsac.penalty;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import org.bukkit.entity.Player;
import wtf.mlsac.penalty.engine.AnimationManager;

/**
 * PacketEvents листенер для блокировки всех пакетов во время анимации.
 * Более надежная альтернатива Bukkit events - работает на уровне пакетов.
 */
public class AnimationPacketListener extends PacketListenerAbstract {
    private final AnimationManager animationManager;

    public AnimationPacketListener(AnimationManager animationManager) {
        super(PacketListenerPriority.HIGHEST);
        this.animationManager = animationManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        
        // Проверяем анимируется ли игрок
        if (animationManager == null || !animationManager.isAnimating(player)) {
            return;
        }

        // Блокируем все пакеты связанные с инвентарем и действиями
        PacketType.Play.Client packetType = (PacketType.Play.Client) event.getPacketType();
        
        if (packetType == PacketType.Play.Client.CLICK_WINDOW ||
            packetType == PacketType.Play.Client.CLOSE_WINDOW ||
            packetType == PacketType.Play.Client.WINDOW_CONFIRMATION ||
            packetType == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION ||
            packetType == PacketType.Play.Client.PICK_ITEM ||
            packetType == PacketType.Play.Client.HELD_ITEM_CHANGE ||
            packetType == PacketType.Play.Client.PLAYER_DIGGING ||
            packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT ||
            packetType == PacketType.Play.Client.USE_ITEM ||
            packetType == PacketType.Play.Client.INTERACT_ENTITY ||
            packetType == PacketType.Play.Client.ENTITY_ACTION ||
            packetType == PacketType.Play.Client.PLAYER_ABILITIES ||
            packetType == PacketType.Play.Client.CLIENT_STATUS) {
            
            // Отменяем пакет - клиент не сможет ничего сделать
            event.setCancelled(true);
        }
    }
}
