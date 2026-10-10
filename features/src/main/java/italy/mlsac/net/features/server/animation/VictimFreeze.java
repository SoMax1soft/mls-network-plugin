// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import italy.mlsac.net.banimation.FreezeSpec;
import italy.mlsac.net.features.server.catalog.PotionKinds;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

public final class VictimFreeze {

    private final PotionKinds potions;

    public VictimFreeze(PotionKinds potions) {
        this.potions = potions;
    }

    public void apply(Player player, FreezeSpec freeze) {
        give(player, PotionKinds.SLOWNESS, freeze.slowTicks(), freeze.slownessAmplifier());
        give(player, PotionKinds.JUMP_BOOST, freeze.slowTicks(), freeze.jumpAmplifier());
        give(player, PotionKinds.LEVITATION, freeze.levitationTicks(), freeze.levitationAmplifier());
        GameMode mode = player.getGameMode();
        if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    public void lift(Player player) {
        potions.find(PotionKinds.SLOWNESS).ifPresent(player::removePotionEffect);
        potions.find(PotionKinds.JUMP_BOOST).ifPresent(player::removePotionEffect);
    }

    private void give(Player player, String kind, int ticks, int amplifier) {
        if (ticks <= 0) {
            return;
        }
        potions.find(kind).ifPresent(type -> player.addPotionEffect(
                new PotionEffect(type, ticks, amplifier, false, false)));
    }
}
