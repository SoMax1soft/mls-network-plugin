// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.nametag;

import italy.mlsac.net.nametag.MetaEntry;
import italy.mlsac.net.nametag.MetaValue;
import italy.mlsac.net.nametag.NametagPacket;
import italy.mlsac.net.packet.MetadataEntry;
import italy.mlsac.net.packet.PacketSender;
import italy.mlsac.net.packet.VirtualEntity;
import italy.mlsac.net.packet.VirtualEntityType;
import italy.mlsac.net.text.LegacyColorizer;
import italy.mlsac.net.workload.WorkloadCounters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class NametagWire {

    private final PacketSender sender;
    private final LegacyColorizer colors;
    private final WorkloadCounters counters;

    public NametagWire(PacketSender sender, LegacyColorizer colors) {
        this(sender, colors, new WorkloadCounters());
    }

    public NametagWire(PacketSender sender, LegacyColorizer colors, WorkloadCounters counters) {
        this.sender = sender;
        this.colors = colors;
        this.counters = counters;
    }

    public void send(UUID viewer, List<NametagPacket> packets) {
        if (packets.isEmpty()) {
            return;
        }
        long began = System.nanoTime();
        for (NametagPacket packet : packets) {
            write(viewer, packet);
        }
        sender.flush(viewer);
        counters.hologramPacketsWritten(packets.size());
        counters.hologramWork(System.nanoTime() - began);
    }

    private void write(UUID viewer, NametagPacket packet) {
        if (packet instanceof NametagPacket.SpawnTextDisplay spawn) {
            sender.spawnVirtual(viewer, new VirtualEntity(spawn.entityNumber(), spawn.uuid(),
                    VirtualEntityType.TEXT_DISPLAY, spawn.x(), spawn.y(), spawn.z()));
        } else if (packet instanceof NametagPacket.SpawnArmorStand spawn) {
            sender.spawnVirtual(viewer, new VirtualEntity(spawn.entityNumber(), spawn.uuid(),
                    VirtualEntityType.ARMOR_STAND, spawn.x(), spawn.y(), spawn.z()));
        } else if (packet instanceof NametagPacket.Metadata metadata) {
            sender.updateVirtualMetadata(viewer, metadata.entityNumber(),
                    metadata.entries().stream().map(this::entry).toList());
        } else if (packet instanceof NametagPacket.Passengers passengers) {
            sender.setPassengers(viewer, passengers.mountNumber(),
                    passengers.passengerIds().stream().mapToInt(Integer::intValue).toArray());
        } else if (packet instanceof NametagPacket.RelativeMove move) {
            sender.moveVirtual(viewer, move.entityNumber(), move.dx(), move.dy(), move.dz());
        } else if (packet instanceof NametagPacket.Teleport teleport) {
            sender.teleportVirtual(viewer, teleport.entityNumber(), teleport.x(), teleport.y(), teleport.z());
        } else if (packet instanceof NametagPacket.Destroy destroy) {
            sender.destroyVirtual(viewer, destroy.entityNumbers().stream().mapToInt(Integer::intValue).toArray());
        }
    }

    private MetadataEntry entry(MetaEntry entry) {
        int index = entry.index();
        MetaValue value = entry.value();
        if (value instanceof MetaValue.ByteValue item) {
            return new MetadataEntry.ByteValue(index, item.value());
        }
        if (value instanceof MetaValue.IntValue item) {
            return new MetadataEntry.IntValue(index, item.value());
        }
        if (value instanceof MetaValue.FloatValue item) {
            return new MetadataEntry.FloatValue(index, item.value());
        }
        if (value instanceof MetaValue.BooleanValue item) {
            return new MetadataEntry.BooleanValue(index, item.value());
        }
        if (value instanceof MetaValue.Vector3fValue item) {
            return new MetadataEntry.VectorValue(index, item.x(), item.y(), item.z());
        }
        if (value instanceof MetaValue.TextValue item) {
            return new MetadataEntry.TextValue(index, colors.colorize(item.sectionSignText()));
        }
        MetaValue.OptionalTextValue item = (MetaValue.OptionalTextValue) value;
        return new MetadataEntry.OptionalTextValue(index, Optional.of(colors.colorize(item.sectionSignText())));
    }
}
