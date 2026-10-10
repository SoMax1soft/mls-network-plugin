// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.notice;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.config.CleanText;
import italy.mlsac.net.json.JsonArray;
import italy.mlsac.net.json.JsonNumber;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonString;
import italy.mlsac.net.json.JsonValue;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class InterServerFeed {

    private static final int DEDUPE_WINDOW = 512;
    private static final List<String> REPORT_FIELDS = List.of("reporterName", "reporter", "reason");

    private final EventBus bus;
    private final Set<String> seenIds = new LinkedHashSet<>();

    public InterServerFeed(EventBus bus) {
        this.bus = bus;
    }

    public void accept(JsonObject reply) {
        Optional<JsonArray> events = reply.object("data").flatMap(data -> data.array("interserverEvents"));
        if (events.isEmpty()) {
            return;
        }
        for (JsonValue value : events.get().elements()) {
            if (value instanceof JsonObject event) {
                toAlert(event).ifPresent(bus::publish);
            }
        }
    }

    private Optional<InterServerAlert> toAlert(JsonObject event) {
        String id = text(event, "id", Integer.MAX_VALUE, "");
        if (!id.isEmpty() && !firstSighting(id)) {
            return Optional.empty();
        }
        String type = text(event, "type", 24, "alert");
        String model = text(event, "model", 32, "unknown");
        String action = text(event, "action", 48, type);
        if (isRetiredReport(event, type, model, action)) {
            return Optional.empty();
        }
        return Optional.of(new InterServerAlert(id, type,
                text(event, "serverName", 32, "unknown"),
                text(event, "playerName", 32, "Unknown"),
                model,
                number(event, "probability").orElse(0.0),
                number(event, "buffer").orElse(0.0),
                (int) Math.round(number(event, "violationLevel").or(() -> number(event, "vl")).orElse(0.0)),
                action));
    }

    private static boolean isRetiredReport(JsonObject event, String type, String model, String action) {
        for (String field : REPORT_FIELDS) {
            if (event.has(field)) {
                return true;
            }
        }
        return containsReport(type) || containsReport(model) || containsReport(action);
    }

    private static boolean containsReport(String value) {
        return value.toLowerCase(Locale.ROOT).contains("report");
    }

    private synchronized boolean firstSighting(String id) {
        if (!seenIds.add(id)) {
            return false;
        }
        if (seenIds.size() > DEDUPE_WINDOW) {
            Iterator<String> oldest = seenIds.iterator();
            oldest.next();
            oldest.remove();
        }
        return true;
    }

    private static String text(JsonObject event, String field, int maxLength, String fallback) {
        Optional<JsonValue> value = event.get(field);
        if (value.isEmpty() || !(value.get() instanceof JsonString string)) {
            return value.filter(JsonNumber.class::isInstance).map(v -> ((JsonNumber) v).literal()).orElse(fallback);
        }
        String cleaned = CleanText.of(string.value(), maxLength);
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private static Optional<Double> number(JsonObject event, String field) {
        return event.number(field).map(JsonNumber::doubleValue).filter(Double::isFinite);
    }
}
