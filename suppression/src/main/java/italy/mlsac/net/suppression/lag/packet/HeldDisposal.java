// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.lag.packet;

@FunctionalInterface
public interface HeldDisposal<P> {

    void dispose(P packet);

    static <P> HeldDisposal<P> forgetting() {
        return packet -> {
        };
    }
}
