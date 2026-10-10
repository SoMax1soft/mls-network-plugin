// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public sealed interface PlannedAction permits InGameAction, CommandAction {

    int stage();

    void placeInto(StageBuilder builder);
}
