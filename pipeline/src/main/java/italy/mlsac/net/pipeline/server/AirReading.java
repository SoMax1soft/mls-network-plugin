// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import org.bukkit.World;

public interface AirReading {

    AirReading UNREADABLE = (world, x, y, z) -> false;

    boolean air(World world, int x, int y, int z);

    static AirReading forRunningServer() {
        return MethodPresence.declares(World.class, "getType", int.class, int.class, int.class)
                ? new BlockTypeAir() : UNREADABLE;
    }
}
