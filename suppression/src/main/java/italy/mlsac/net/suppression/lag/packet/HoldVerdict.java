// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.packet;

import java.util.List;

public record HoldVerdict<P>(PacketDisposition disposition, List<P> releaseNow) {

    public HoldVerdict {
        releaseNow = List.copyOf(releaseNow);
    }

    public static <P> HoldVerdict<P> pass() {
        return new HoldVerdict<>(PacketDisposition.PASS, List.of());
    }

    public static <P> HoldVerdict<P> passAfter(List<P> releaseNow) {
        return new HoldVerdict<>(PacketDisposition.PASS, releaseNow);
    }

    public static <P> HoldVerdict<P> hold(List<P> releaseNow) {
        return new HoldVerdict<>(PacketDisposition.HOLD, releaseNow);
    }
}
