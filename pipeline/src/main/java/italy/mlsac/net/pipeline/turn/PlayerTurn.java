// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.turn;

import java.util.Arrays;

public final class PlayerTurn {
    private static final TurnPart[] PARTS = TurnPart.values();

    private final TurnEpoch epochs;
    private final Object[] parts = new Object[PARTS.length];
    private long epoch = Long.MIN_VALUE;

    public PlayerTurn() {
        this(new TurnEpoch());
    }

    public PlayerTurn(TurnEpoch epochs) {
        this.epochs = epochs;
    }

    public Object part(TurnPart part) {
        long now = epochs.value();
        if (epoch != now) {
            Arrays.fill(parts, null);
            epoch = now;
        }
        return parts[part.ordinal()];
    }

    public void keep(TurnPart part, Object value) {
        parts[part.ordinal()] = value;
    }

    public void clear() {
        Arrays.fill(parts, null);
    }
}
