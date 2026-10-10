// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import italy.mlsac.net.suppression.lag.runtime.BodyCondition;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public final class BodyConditions {

    private static final Set<String> ALWAYS_SUBMERGED = Set.of("BUBBLE_COLUMN", "KELP", "KELP_PLANT", "SEAGRASS",
            "TALL_SEAGRASS");

    private final List<ConditionProbe> probes;

    public BodyConditions(ClimbCheck climbCheck) {
        this.probes = List.of(
                new ConditionProbe(BodyCondition.GLIDING, Player::isGliding),
                new ConditionProbe(BodyCondition.SWIMMING, Player::isSwimming),
                new ConditionProbe(BodyCondition.IN_LIQUID, BodyConditions::submerged),
                new ConditionProbe(BodyCondition.CLIMBING, climbCheck::climbing),
                new ConditionProbe(BodyCondition.FLYING, Player::isFlying),
                new ConditionProbe(BodyCondition.IN_VEHICLE, Player::isInsideVehicle),
                new ConditionProbe(BodyCondition.SLEEPING, Player::isSleeping),
                new ConditionProbe(BodyCondition.DEAD, Player::isDead),
                new ConditionProbe(BodyCondition.CREATIVE_OR_SPECTATOR, BodyConditions::creativeOrSpectator),
                new ConditionProbe(BodyCondition.LEVITATING,
                        player -> player.hasPotionEffect(PotionEffectType.LEVITATION)),
                new ConditionProbe(BodyCondition.SLOW_FALLING,
                        player -> player.hasPotionEffect(PotionEffectType.SLOW_FALLING)),
                new ConditionProbe(BodyCondition.RIPTIDE, Player::isRiptiding));
    }

    public Set<BodyCondition> of(Player player) {
        EnumSet<BodyCondition> present = EnumSet.noneOf(BodyCondition.class);
        for (ConditionProbe probe : probes) {
            if (probe.test().test(player)) {
                present.add(probe.condition());
            }
        }
        return present;
    }

    private static boolean creativeOrSpectator(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    }

    private static boolean submerged(Player player) {
        return liquid(player.getLocation().getBlock()) || liquid(player.getEyeLocation().getBlock());
    }

    private static boolean liquid(Block block) {
        if (block.isLiquid() || ALWAYS_SUBMERGED.contains(block.getType().name())) {
            return true;
        }
        BlockData data = block.getBlockData();
        return data instanceof Waterlogged soaked && soaked.isWaterlogged();
    }

    private record ConditionProbe(BodyCondition condition, Predicate<Player> test) {
    }
}
