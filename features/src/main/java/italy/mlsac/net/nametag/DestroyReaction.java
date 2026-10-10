// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.List;

public record DestroyReaction(List<Integer> idsToAppend, List<NametagPacket> packetsAfter) {

    public DestroyReaction {
        idsToAppend = List.copyOf(idsToAppend);
        packetsAfter = List.copyOf(packetsAfter);
    }

    public static DestroyReaction none() {
        return new DestroyReaction(List.of(), List.of());
    }

    public boolean rewritesPacket() {
        return !idsToAppend.isEmpty();
    }
}
