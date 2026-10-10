// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.ArrayList;
import java.util.List;

public final class TextDisplayRenderer implements OverheadTagPainter {

    static final double SPAWN_LIFT = 1.8;
    static final float SEAT_OFFSET_SHIFTED = 0.72f;
    static final float SEAT_OFFSET_CLASSIC = 1.15f;
    static final int LINE_WIDTH = 1000;
    static final float VIEW_RANGE_UNIT = 64f;

    private final int shift;
    private final float seatOffset;

    public TextDisplayRenderer(GameRelease server) {
        boolean shifted = server.atLeast(GameRelease.SHIFTED_DISPLAY_INDICES);
        this.shift = shifted ? 1 : 0;
        this.seatOffset = shifted ? SEAT_OFFSET_SHIFTED : SEAT_OFFSET_CLASSIC;
    }

    @Override
    public TagEntities claim(TagEntities available, TagLines text) {
        return available.lowest(1);
    }

    @Override
    public List<NametagPacket> spawn(ShownTag tag, SuspectView suspect, NametagSettings settings) {
        NametagDisplay display = settings.display();
        float scale = (float) display.scale();
        float lift = seatOffset + (float) tag.extraHeight();
        NametagIdentity entity = tag.entities().bottom();
        List<MetaEntry> entries = List.of(
                new MetaEntry(10 + shift, new MetaValue.Vector3fValue(0f, lift, 0f)),
                new MetaEntry(11 + shift, new MetaValue.Vector3fValue(scale, scale, scale)),
                new MetaEntry(14 + shift, new MetaValue.ByteValue(display.billboard().metadataValue())),
                new MetaEntry(16 + shift, new MetaValue.FloatValue(settings.limits().seenWithinBlocks() / VIEW_RANGE_UNIT)),
                new MetaEntry(22 + shift, new MetaValue.TextValue(tag.text().stacked())),
                new MetaEntry(23 + shift, new MetaValue.IntValue(LINE_WIDTH)),
                new MetaEntry(24 + shift, new MetaValue.IntValue(display.plateColour())),
                new MetaEntry(26 + shift, new MetaValue.ByteValue(display.flags())));
        return List.of(
                new NametagPacket.SpawnTextDisplay(entity.entityNumber(), entity.uuid(), suspect.x(),
                        suspect.y() + SPAWN_LIFT, suspect.z()),
                new NametagPacket.Metadata(entity.entityNumber(), entries),
                new NametagPacket.Passengers(suspect.suspectEntityId(), passengersWithTag(tag, suspect.passengers())));
    }

    @Override
    public List<NametagPacket> text(ShownTag tag) {
        return List.of(new NametagPacket.Metadata(tag.entities().bottom().entityNumber(),
                List.of(new MetaEntry(22 + shift, new MetaValue.TextValue(tag.text().stacked())))));
    }

    @Override
    public List<NametagPacket> relativeMove(ShownTag tag, double dx, double dy, double dz) {
        return List.of();
    }

    @Override
    public List<NametagPacket> teleport(ShownTag tag, SuspectView suspect) {
        return List.of();
    }

    @Override
    public List<NametagPacket> poseChanged(ShownTag tag, SuspectView suspect, SuspectPose previous) {
        return List.of();
    }

    @Override
    public List<NametagPacket> passengerRepair(ShownTag tag, SuspectView suspect) {
        return List.of(new NametagPacket.Passengers(suspect.suspectEntityId(),
                passengersWithTag(tag, suspect.passengers())));
    }

    @Override
    public List<Integer> passengersWithTag(ShownTag tag, List<Integer> serverPassengers) {
        int rider = tag.entities().bottom().entityNumber();
        List<Integer> passengers = new ArrayList<>(serverPassengers.size() + 1);
        for (int passenger : serverPassengers) {
            if (passenger != rider) {
                passengers.add(passenger);
            }
        }
        passengers.add(rider);
        return passengers;
    }
}
