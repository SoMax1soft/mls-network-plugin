// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.region.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import italy.mlsac.net.region.BlockPosition;
import italy.mlsac.net.region.RegionLookup;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class WorldGuardRegionLookup implements RegionLookup {

    private WorldGuardRegionLookup() {
    }

    public static RegionLookup detect() {
        try {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
                return RegionLookup.NONE;
            }
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return new WorldGuardRegionLookup();
        } catch (ClassNotFoundException | LinkageError missing) {
            return RegionLookup.NONE;
        }
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public Optional<String> topRegion(BlockPosition position) {
        World world = Bukkit.getWorld(position.worldUuid());
        if (world == null) {
            return Optional.empty();
        }
        ApplicableRegionSet regions = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery()
                .getApplicableRegions(BukkitAdapter.adapt(new Location(world, position.x(), position.y(), position.z())));
        ProtectedRegion top = null;
        for (ProtectedRegion region : regions) {
            if (top == null || region.getPriority() > top.getPriority()) {
                top = region;
            }
        }
        return top == null ? Optional.empty() : Optional.of(top.getId().toLowerCase(Locale.ROOT));
    }
}
