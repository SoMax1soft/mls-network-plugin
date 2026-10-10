// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.List;

public record SuspectView(int suspectEntityId, double x, double y, double z, SuspectPose pose,
                          List<Integer> passengers) {

    public SuspectView {
        passengers = List.copyOf(passengers);
    }
}
