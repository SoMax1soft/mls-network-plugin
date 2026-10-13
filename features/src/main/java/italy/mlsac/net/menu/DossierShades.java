// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.dossier.DossierTimes;
import italy.mlsac.net.dossier.HistoryEntry;
import italy.mlsac.net.text.TemplateValues;

final class DossierShades {

    static final String BAN = "ban";
    static final String ANIMATION = "animation";
    static final String NONE = "-";

    private DossierShades() {
    }

    static String shownAction(String action) {
        return ANIMATION.equals(action) ? BAN : action;
    }

    static String shownModel(String model) {
        return model.isEmpty() ? NONE : model;
    }

    static TemplateValues entryValues(HistoryEntry entry, DossierTimes times) {
        return TemplateValues.of("{ACTION}", shownAction(entry.action()))
                .and("{MODEL}", shownModel(entry.model()))
                .and("{PERCENT}", ValueShade.roundedPercent(entry.probability()))
                .and("{PROB}", ValueShade.twoDecimals(entry.probability()))
                .and("{TIME}", times.stamp(entry.at()))
                .and("{AGO}", times.ago(entry.at()))
                .and("{TRIGGER}", entry.trigger())
                .and("{LEVEL}", entry.level())
                .and("{BUFFER}", ValueShade.twoDecimals(entry.buffer()))
                .and("{ACTIONS}", entry.actions().isEmpty() ? NONE : String.join(", ", entry.actions()))
                .and("{KIND}", entry.kind());
    }
}
