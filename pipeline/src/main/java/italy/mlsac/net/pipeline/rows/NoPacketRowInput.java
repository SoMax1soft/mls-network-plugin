// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.rotation.RotationChain;
import italy.mlsac.net.pipeline.rotation.RotationStep;

public enum NoPacketRowInput {
    CONSUMED {
        @Override
        boolean takesInput(RowOrigin origin) {
            return true;
        }

        @Override
        RotationStep stepWithoutRotation(RotationChain chain, RowOrigin origin) {
            return chain.still();
        }
    },
    CARRIED_TO_NEXT_PACKET {
        @Override
        boolean takesInput(RowOrigin origin) {
            return !origin.noPacket();
        }

        @Override
        RotationStep stepWithoutRotation(RotationChain chain, RowOrigin origin) {
            return origin.noPacket() ? RotationStep.ABSENT : chain.still();
        }
    };

    abstract boolean takesInput(RowOrigin origin);

    abstract RotationStep stepWithoutRotation(RotationChain chain, RowOrigin origin);
}
