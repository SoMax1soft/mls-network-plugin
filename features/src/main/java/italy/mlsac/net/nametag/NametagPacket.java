// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.List;
import java.util.UUID;

public sealed interface NametagPacket permits NametagPacket.SpawnTextDisplay, NametagPacket.SpawnArmorStand,
        NametagPacket.Metadata, NametagPacket.Passengers, NametagPacket.RelativeMove, NametagPacket.Teleport,
        NametagPacket.Destroy {

    record SpawnTextDisplay(int entityNumber, UUID uuid, double x, double y, double z) implements NametagPacket {
    }

    record SpawnArmorStand(int entityNumber, UUID uuid, double x, double y, double z, SpawnPacketKind kind)
            implements NametagPacket {
    }

    record Metadata(int entityNumber, List<MetaEntry> entries) implements NametagPacket {

        public Metadata {
            entries = List.copyOf(entries);
        }
    }

    record Passengers(int mountNumber, List<Integer> passengerIds) implements NametagPacket {

        public Passengers {
            passengerIds = List.copyOf(passengerIds);
        }
    }

    record RelativeMove(int entityNumber, double dx, double dy, double dz) implements NametagPacket {
    }

    record Teleport(int entityNumber, double x, double y, double z) implements NametagPacket {
    }

    record Destroy(List<Integer> entityNumbers) implements NametagPacket {

        public Destroy {
            entityNumbers = List.copyOf(entityNumbers);
        }
    }
}
