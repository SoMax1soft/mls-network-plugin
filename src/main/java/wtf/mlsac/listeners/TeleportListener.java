/*
 * This file is part of MLSAC - AI powered Anti-Cheat
 * Copyright (C) 2026 MLSAC Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This file contains code derived from:
 *   - SlothAC (© 2025 KaelusMC, https://github.com/KaelusMC/SlothAC)
 *   - Grim (© 2025 GrimAnticheat, https://github.com/GrimAnticheat/Grim)
 * All derived code is licensed under GPL-3.0.
 */


package wtf.mlsac.listeners;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import wtf.mlsac.Main;
import wtf.mlsac.checks.AICheck;
import wtf.mlsac.penalty.engine.AnimationManager;

public class TeleportListener implements Listener {
    private final AICheck aiCheck;
    private final Main plugin;
    
    public TeleportListener(AICheck aiCheck, Main plugin) {
        this.aiCheck = aiCheck;
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (aiCheck == null) return;
        
        Player player = event.getPlayer();
        
        // Не сбрасываем данные если игрок анимируется (плавное перемещение во время бан-анимации)
        AnimationManager animationManager = plugin.getAnimationManager();
        if (animationManager != null && animationManager.isAnimating(player)) {
            return;
        }
        
        aiCheck.onTeleport(player);
    }
}