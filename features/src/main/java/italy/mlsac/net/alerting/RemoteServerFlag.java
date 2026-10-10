// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import java.util.Locale;
import java.util.Optional;

public record RemoteServerFlag(String type, String model, String action, String hostLabel, String playerNick,
                               int offenceLevel, double probability, double buffer) {

    private static final String REPORT = "report";

    public static Optional<RemoteServerFlag> from(RemoteEvent event) {
        String type = clean(event.type(), 24).orElse("alert");
        String model = clean(event.model(), 32).orElse("unknown");
        String action = clean(event.action(), 48).orElse(type);
        if (event.carriesReportFields() || mentionsReport(type) || mentionsReport(model) || mentionsReport(action)) {
            return Optional.empty();
        }
        String server = clean(event.hostLabel(), 32).orElse("unknown");
        String player = clean(event.playerNick(), 32).orElse("Unknown");
        double level = event.offenceLevel().isPresent() ? event.offenceLevel().getAsDouble()
                : event.vl().orElse(0.0);
        return Optional.of(new RemoteServerFlag(type, model, action, server, player, (int) Math.round(level),
                event.probability(), event.buffer()));
    }

    private static Optional<String> clean(Optional<String> raw, int maxLength) {
        return raw.map(text -> withoutControls(text, maxLength).trim()).filter(text -> !text.isEmpty());
    }

    private static String withoutControls(String text, int maxLength) {
        StringBuilder out = new StringBuilder(Math.min(text.length(), maxLength));
        for (int i = 0; i < text.length() && out.length() < maxLength; i++) {
            char c = text.charAt(i);
            if (c >= 0x20 && c != 0x7F) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean mentionsReport(String value) {
        return value.toLowerCase(Locale.ROOT).contains(REPORT);
    }
}
