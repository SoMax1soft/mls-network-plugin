// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.DisplayNumbers;
import italy.mlsac.net.text.TemplateValues;
import italy.mlsac.net.scoring.SuspectSnapshot;
import italy.mlsac.net.scoring.Trend;
import java.util.List;
import java.util.Map;

public final class ActionBarText {

    private final MessageBook messages;

    public ActionBarText(MessageBook messages) {
        this.messages = messages;
    }

    public String build(SuspectSnapshot suspect, int offenceLevel) {
        String status = messages.rawText(suspect.trend() == Trend.RISING ? "STATUS_UP" : "STATUS_DOWN");
        TemplateValues values = TemplateValues.of("{PLAYER}", suspect.name())
                .and("{PROBABILITY}", DisplayNumbers.percent(suspect.latestScore()) + "%")
                .and("{BUFFER}", DisplayNumbers.bufferInServerLocale(suspect.buffer()))
                .and("{VL}", offenceLevel)
                .and("{MODEL}", DisplayNumbers.percent(suspect.latestScore()))
                .and("{AVG}", DisplayNumbers.percent(suspect.average()))
                .aliases(DisplayNumbers.percent(fastOrMini(suspect.lastByModel())), "{LAST-FAST}", "{FAST}")
                .and("{LAST-PRO}", DisplayNumbers.percent(last(suspect.lastByModel(), "pro")))
                .and("{LAST-ULTRA}", DisplayNumbers.percent(last(suspect.lastByModel(), "ultra")))
                .and("{AVG-FAST}", DisplayNumbers.percent(averageFastOrMini(suspect.historyByModel())))
                .and("{AVG-PRO}", DisplayNumbers.percent(average(suspect.historyByModel(), "pro")))
                .and("{AVG-ULTRA}", DisplayNumbers.percent(average(suspect.historyByModel(), "ultra")))
                .and("{STATUS}", status);
        return messages.colorize(values.apply(messages.rawText("actionbar-format")));
    }

    public String unscored(String targetName) {
        return messages.text("monitor-no-data", TemplateValues.of("{PLAYER}", targetName));
    }

    private static double fastOrMini(Map<String, Double> lastByModel) {
        double fast = last(lastByModel, "fast");
        return fast == 0.0 ? last(lastByModel, "mini") : fast;
    }

    private static double averageFastOrMini(Map<String, List<Double>> history) {
        double fast = average(history, "fast");
        return fast == 0.0 ? average(history, "mini") : fast;
    }

    private static double last(Map<String, Double> lastByModel, String marker) {
        return lastByModel.entrySet().stream().filter(entry -> entry.getKey().contains(marker))
                .map(Map.Entry::getValue).findFirst().orElse(0.0);
    }

    private static double average(Map<String, List<Double>> history, String marker) {
        return history.entrySet().stream().filter(entry -> entry.getKey().contains(marker))
                .map(entry -> SuspectSnapshot.mean(entry.getValue())).findFirst().orElse(0.0);
    }
}
