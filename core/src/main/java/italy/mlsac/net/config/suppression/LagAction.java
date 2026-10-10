// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import java.util.List;

public record LagAction(int stage, LagLevel level, List<LagEffect> effects, int seconds)
        implements SuppressionAction, TimedAction {

    public static final String TYPE = "lag";

    public LagAction {
        effects = List.copyOf(effects);
    }

    @Override
    public String kindKey() {
        return TYPE;
    }
}
