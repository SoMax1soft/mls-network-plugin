// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import italy.mlsac.net.config.SettingsLayer;
import italy.mlsac.net.config.response.TrickKind;
import java.util.Optional;

final class TrickActionParser implements SuppressionVerbReader {

    private final TrickKind kind;

    TrickActionParser(TrickKind kind) {
        this.kind = kind;
    }

    @Override
    public String type() {
        return kind.id();
    }

    @Override
    public boolean allowedInBufferRules() {
        return true;
    }

    @Override
    public Optional<SuppressionAction> parse(SettingsLayer action, int stage) {
        return Optional.of(new TrickAction(stage, kind));
    }
}
