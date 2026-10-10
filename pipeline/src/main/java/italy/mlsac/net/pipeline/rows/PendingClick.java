// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.rows;

import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.player.PlayerSample;

record PendingClick(
        long nanos,
        int entityNumber,
        boolean cancelled,
        PlayerSample target,
        PlayerSample own,
        Vec3 storedPosition,
        boolean grounded,
        AttackHistory history) {

    PlayerSample backupEyeSample() {
        if (own == null) {
            return null;
        }
        return storedPosition == null ? own : own.movedTo(storedPosition.x(), storedPosition.y(), storedPosition.z());
    }
}
