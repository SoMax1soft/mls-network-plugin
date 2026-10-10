// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trick;

import italy.mlsac.net.suppression.plan.TrickTuning;
import italy.mlsac.net.suppression.plan.TrickType;
import italy.mlsac.net.suppression.server.SuspectBody;

public final class ShuffleInventoryTrick implements TrickEffect {

    private final StoragePermutation permutation;

    public ShuffleInventoryTrick(StoragePermutation permutation) {
        this.permutation = permutation;
    }

    @Override
    public TrickType type() {
        return TrickType.SHUFFLE_INVENTORY;
    }

    @Override
    public boolean perform(SuspectBody body, TrickTuning tuning) {
        if (body.dead() || body.creativeOrSpectator()) {
            return false;
        }
        body.permuteStorage(permutation.draw(body.storageSize()));
        return true;
    }
}
