// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

public record TrickAction(int stage, TrickType type) implements InGameAction {

    @Override
    public void offerTo(InGameActionSink sink) {
        sink.trick(this);
    }
}
