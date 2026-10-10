// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.List;

public interface OverheadTagPainter {

    TagEntities claim(TagEntities available, TagLines text);

    List<NametagPacket> spawn(ShownTag tag, SuspectView suspect, NametagSettings settings);

    List<NametagPacket> text(ShownTag tag);

    List<NametagPacket> relativeMove(ShownTag tag, double dx, double dy, double dz);

    List<NametagPacket> teleport(ShownTag tag, SuspectView suspect);

    List<NametagPacket> poseChanged(ShownTag tag, SuspectView suspect, SuspectPose previous);

    List<NametagPacket> passengerRepair(ShownTag tag, SuspectView suspect);

    List<Integer> passengersWithTag(ShownTag tag, List<Integer> serverPassengers);
}
