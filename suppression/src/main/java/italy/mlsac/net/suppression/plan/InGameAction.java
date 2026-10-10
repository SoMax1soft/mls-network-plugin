// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public sealed interface InGameAction extends PlannedAction permits TimedAction, TrickAction {

    void offerTo(InGameActionSink sink);

    @Override
    default void placeInto(StageBuilder builder) {
        builder.inGame(this);
    }
}
