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

    public Optional<StaffTemplate> templateFor(TrickType type, TrickTuning tuning) {
        Optional<String> override = texts.presetOverride(type.profile().messageKey());
        if (override.isPresent()) {
            return prefixed(override.get());
        }
        String legacy = tuning.legacyMessage();
        if (!legacy.isBlank() && !stock.isStock(legacy)) {
            return Optional.of(StaffTemplate.bare(legacy));
        }
        return prefixed(texts.bundled(type.profile().messageKey()));
    }

    private Optional<StaffTemplate> prefixed(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new StaffTemplate(texts.prefix(), text));
    }
}
