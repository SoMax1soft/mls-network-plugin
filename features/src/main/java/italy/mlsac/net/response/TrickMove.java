// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.response;

import java.util.Optional;
import java.util.UUID;

public enum TrickMove {
    SHUFFLE_INVENTORY("shuffle_inventory") {
        @Override
        boolean perform(TrickHands hands, UUID player, TrollTrick trick) {
            return hands.scrambleInventory(player);
        }
    },
    DROP_WEAPON("drop_weapon") {
        @Override
        boolean perform(TrickHands hands, UUID player, TrollTrick trick) {
            return hands.flingHeldItem(player, trick.bladeRequired(), trick.sidewaysPush(), trick.upwardPush());
        }
    },
    LAUNCH("launch") {
        @Override
        boolean perform(TrickHands hands, UUID player, TrollTrick trick) {
            return hands.launch(player, trick.sidewaysPush(), trick.upwardPush());
        }
    };

    private final String id;

    TrickMove(String id) {
        this.id = id;
    }

    public static Optional<TrickMove> of(String type) {
        for (TrickMove move : values()) {
            if (move.id.equals(type)) {
                return Optional.of(move);
            }
        }
        return Optional.empty();
    }

    public String id() {
        return id;
    }

    abstract boolean perform(TrickHands hands, UUID player, TrollTrick trick);
}
