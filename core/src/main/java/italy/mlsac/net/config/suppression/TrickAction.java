// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import italy.mlsac.net.config.response.TrickKind;

public record TrickAction(int stage, TrickKind kind) implements SuppressionAction {

    @Override
    public String kindKey() {
        return kind.id();
    }
}
