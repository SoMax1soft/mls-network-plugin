// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.detection.BadPacketKind;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.DisplayNumbers;
import italy.mlsac.net.text.TemplateValues;

public final class AlertTexts {

    public static final String FLAG_MODEL = "Unknown";

    private final MessageBook messages;

    public AlertTexts(MessageBook messages) {
        this.messages = messages;
    }

    public String stepAlert(StepAlertTemplate template, String player, double probability, double buffer,
                            String model, long stepPercent) {
        return messages.prefixed(template.messageKey(), common(player, probability, buffer, 0, model)
                .aliases(stepPercent, "{STEP_PERCENT}", "<step_percent>"));
    }

    public String flagAlert(String player, double probability, double buffer, int offenceLevel) {
        return messages.prefixed("alert-format-vl", common(player, probability, buffer, offenceLevel, FLAG_MODEL));
    }

    public String badPackets(String player, BadPacketKind kind, int count, int windowSeconds) {
        return messages.prefixed("alert-bad-packets", TemplateValues.none()
                .aliases(player, "{PLAYER}", "<player>")
                .and("{CHECK}", messages.rawText("bad-packets-" + kind.slug()))
                .and("{COUNT}", count)
                .and("{WINDOW}", windowSeconds));
    }

    public String monitorLine(String model, String player, double probability) {
        return messages.text("monitor-format", TemplateValues.of("{MODEL}", model)
                .and("{PLAYER}", player)
                .and("{PROBABILITY_COLORED}", MonitorShade.colored(probability)));
    }

    public String crossServer(RemoteServerFlag alert, String modelDisplay) {
        return messages.prefixed("interserver-alert-format",
                common(alert.playerNick(), alert.probability(), alert.buffer(), alert.offenceLevel(), modelDisplay)
                        .and("{ACTION}", alert.action())
                        .aliases(alert.hostLabel(), "{SERVER}", "{SERVER_NAME}", "{SOURCE_SERVER}", "<server>"));
    }

    public String strip(String colored) {
        return messages.strip(colored);
    }

    private static TemplateValues common(String player, double probability, double buffer, int offenceLevel,
                                       String model) {
        return TemplateValues.none()
                .aliases(player, "{PLAYER}", "<player>")
                .aliases(DisplayNumbers.percent(probability) + "%", "{PROBABILITY}", "<probability>")
                .aliases(DisplayNumbers.bufferInServerLocale(buffer), "{BUFFER}", "<buffer>")
                .aliases(offenceLevel, "{VL}", "<vl>")
                .aliases(model, "{MODEL}", "<model>");
    }
}
