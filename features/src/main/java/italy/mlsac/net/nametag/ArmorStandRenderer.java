// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.ArrayList;
import java.util.List;

public final class ArmorStandRenderer implements OverheadTagPainter {

    static final byte INVISIBLE = 0x20;
    static final byte MARKER = 0x10;
    static final double LINE_SPACING = 0.27;

    private final SpawnPacketKind spawnKind;
    private final int armorStandMaskSlot;

    public ArmorStandRenderer(GameRelease server) {
        this.spawnKind = SpawnPacketKind.forServer(server);
        this.armorStandMaskSlot = armorStandMaskSlot(server);
    }

    static int armorStandMaskSlot(GameRelease server) {
        if (server.atLeast(new GameRelease(1, 17, 0))) {
            return 15;
        }
        if (server.atLeast(new GameRelease(1, 15, 0))) {
            return 14;
        }
        if (server.atLeast(new GameRelease(1, 14, 0))) {
            return 13;
        }
        return 11;
    }

    @Override
    public TagEntities claim(TagEntities available, TagLines text) {
        return available.lowest(text.count());
    }

    @Override
    public List<NametagPacket> spawn(ShownTag tag, SuspectView suspect, NametagSettings settings) {
        List<NametagPacket> packets = new ArrayList<>(tag.entities().count() * 2);
        for (int level = 0; level < tag.entities().count(); level++) {
            NametagIdentity stand = tag.entities().atLevel(level);
            packets.add(new NametagPacket.SpawnArmorStand(stand.entityNumber(), stand.uuid(), suspect.x(),
                    suspect.y() + standHeight(tag, suspect.pose(), level), suspect.z(), spawnKind));
            packets.add(new NametagPacket.Metadata(stand.entityNumber(), List.of(
                    new MetaEntry(0, new MetaValue.ByteValue(INVISIBLE)),
                    new MetaEntry(2, new MetaValue.OptionalTextValue(tag.text().fromBottom(level))),
                    new MetaEntry(3, new MetaValue.BooleanValue(true)),
                    new MetaEntry(armorStandMaskSlot, new MetaValue.ByteValue(MARKER)))));
        }
        return packets;
    }

    @Override
    public List<NametagPacket> text(ShownTag tag) {
        List<NametagPacket> packets = new ArrayList<>(tag.entities().count());
        for (int level = 0; level < tag.entities().count(); level++) {
            packets.add(new NametagPacket.Metadata(tag.entities().atLevel(level).entityNumber(),
                    List.of(new MetaEntry(2, new MetaValue.OptionalTextValue(tag.text().fromBottom(level))))));
        }
        return packets;
    }

    @Override
    public List<NametagPacket> relativeMove(ShownTag tag, double dx, double dy, double dz) {
        List<NametagPacket> packets = new ArrayList<>(tag.entities().count());
        for (int number : tag.entities().numbers()) {
            packets.add(new NametagPacket.RelativeMove(number, dx, dy, dz));
        }
        return packets;
    }

    @Override
    public List<NametagPacket> teleport(ShownTag tag, SuspectView suspect) {
        List<NametagPacket> packets = new ArrayList<>(tag.entities().count());
        for (int level = 0; level < tag.entities().count(); level++) {
            packets.add(new NametagPacket.Teleport(tag.entities().atLevel(level).entityNumber(), suspect.x(),
                    suspect.y() + standHeight(tag, suspect.pose(), level), suspect.z()));
        }
        return packets;
    }

    @Override
    public List<NametagPacket> poseChanged(ShownTag tag, SuspectView suspect, SuspectPose previous) {
        return previous.tagHeight() == suspect.pose().tagHeight() ? List.of() : teleport(tag, suspect);
    }

    @Override
    public List<NametagPacket> passengerRepair(ShownTag tag, SuspectView suspect) {
        return List.of();
    }

    @Override
    public List<Integer> passengersWithTag(ShownTag tag, List<Integer> serverPassengers) {
        return serverPassengers;
    }

    private static double standHeight(ShownTag tag, SuspectPose pose, int level) {
        return pose.tagHeight() + tag.extraHeight() + level * LINE_SPACING;
    }
}
