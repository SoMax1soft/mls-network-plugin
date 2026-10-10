// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;

final class WaterUnderfoot implements PlayerTrait {

    @Override
    public boolean of(Player player) {
        Block feet = player.getLocation().getBlock();
        if (feet.getType() == Material.WATER) {
            return true;
        }
        BlockData shape = feet.getBlockData();
        return shape instanceof Waterlogged soaked && soaked.isWaterlogged();
    }
}
