// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

import italy.mlsac.net.suppression.plan.TrickTuning;
import italy.mlsac.net.suppression.plan.TrickType;
import italy.mlsac.net.suppression.server.SuppressionTexts;
import java.util.Optional;

public final class TrickTextChoice {

    private final SuppressionTexts texts;
    private final StockTrickTexts stock;

    public TrickTextChoice(SuppressionTexts texts, StockTrickTexts stock) {
        this.texts = texts;
        this.stock = stock;
    }

    public Optional<String> templateFor(TrickType type, TrickTuning tuning) {
        Optional<String> override = texts.presetOverride(type.profile().messageKey());
        if (override.isPresent()) {
            return nonBlank(override.get());
        }
        String legacy = tuning.legacyMessage();
        if (!legacy.isBlank() && !stock.isStock(legacy)) {
            return Optional.of(legacy);
        }
        return nonBlank(texts.bundled(type.profile().messageKey()));
    }

    private static Optional<String> nonBlank(String text) {
        return text == null || text.isBlank() ? Optional.empty() : Optional.of(text);
    }
}
