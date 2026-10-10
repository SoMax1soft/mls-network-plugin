// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import italy.mlsac.net.text.DisplayNumbers;
import italy.mlsac.net.text.TemplateValues;
import italy.mlsac.net.scoring.FlagContext;

public final class PenaltyText {

    private PenaltyText() {
    }

    public static String resolve(String text, FlagContext target) {
        return TemplateValues.none()
                .aliases(target.playerNick(), "{PLAYER}", "<player>")
                .aliases(target.offenceLevel(), "{VL}", "<vl>")
                .aliases(DisplayNumbers.twoDecimals(target.probability()), "{PROBABILITY}", "<probability>")
                .aliases(DisplayNumbers.twoDecimals(target.buffer()), "{BUFFER}", "<buffer>")
                .apply(text);
    }
}
