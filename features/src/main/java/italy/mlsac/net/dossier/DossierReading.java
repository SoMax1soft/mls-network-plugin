// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

import italy.mlsac.net.json.JsonArray;
import italy.mlsac.net.json.JsonNumber;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonString;
import italy.mlsac.net.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public final class DossierReading {

    static final int MAX_ENTRIES = 64;
    static final int MAX_ACTIONS = 8;

    private DossierReading() {
    }

    public static KickPage kicks(JsonObject data) {
        List<KickEntry> entries = new ArrayList<>();
        for (JsonObject entry : objects(data, "entries")) {
            entries.add(new KickEntry(text(entry, "player"), whole(entry, "t"), text(entry, "action"),
                    text(entry, "model"), real(entry, "p"), (int) whole(entry, "kicks"), (int) whole(entry, "bans")));
        }
        return new KickPage((int) whole(data, "page"), (int) whole(data, "pages"), (int) whole(data, "total"),
                data.bool("hasMore").orElse(false), entries);
    }

    public static HistoryPage history(JsonObject data) {
        List<HistoryEntry> entries = new ArrayList<>();
        for (JsonObject entry : objects(data, "entries")) {
            entries.add(entry(entry, entry.string("kind").orElse(HistoryEntry.DETECTION)));
        }
        return new HistoryPage(text(data, "player"), (int) whole(data, "page"), (int) whole(data, "pages"),
                (int) whole(data, "total"), data.bool("hasMore").orElse(false), entries);
    }

    public static PlayerDossier profile(JsonObject data) {
        JsonObject detections = section(data, "detections");
        List<ModelTally> models = new ArrayList<>();
        for (JsonObject model : objects(detections, "models")) {
            models.add(new ModelTally(text(model, "model"), (int) whole(model, "count"), real(model, "max")));
        }
        DetectionTally tally = new DetectionTally((int) whole(detections, "total"), (int) whole(detections, "day"),
                (int) whole(detections, "week"), real(detections, "max"), real(detections, "avg"),
                moment(detections, "lastAt"), models);
        JsonObject punishments = section(data, "punishments");
        Optional<HistoryEntry> last = punishments.object("last").map(found -> entry(found, HistoryEntry.PUNISH));
        PunishmentTally punished = new PunishmentTally((int) whole(punishments, "total"),
                (int) whole(punishments, "kicks"), (int) whole(punishments, "bans"), last);
        JsonObject suppressions = section(data, "suppressions");
        List<HistoryEntry> latestSuppressions = new ArrayList<>();
        for (JsonObject entry : objects(suppressions, "latest")) {
            latestSuppressions.add(entry(entry, HistoryEntry.SUPPRESSION));
        }
        List<HistoryEntry> latest = new ArrayList<>();
        for (JsonObject entry : objects(data, "latest")) {
            latest.add(entry(entry, HistoryEntry.DETECTION));
        }
        return new PlayerDossier(text(data, "player"), data.bool("known").orElse(false), moment(data, "firstSeen"),
                moment(data, "lastSeen"), tally, punished, (int) whole(suppressions, "total"), latestSuppressions,
                latest);
    }

    private static HistoryEntry entry(JsonObject entry, String kind) {
        List<String> actions = new ArrayList<>();
        entry.array("actions").ifPresent(list -> {
            for (JsonValue value : list.elements()) {
                if (value instanceof JsonString action && actions.size() < MAX_ACTIONS) {
                    actions.add(action.value());
                }
            }
        });
        return new HistoryEntry(kind, whole(entry, "t"), text(entry, "action"), text(entry, "model"),
                real(entry, "p"), text(entry, "trigger"), (int) whole(entry, "level"), real(entry, "buffer"),
                actions);
    }

    private static JsonObject section(JsonObject owner, String name) {
        return owner.object(name).orElseGet(() -> JsonObject.builder().build());
    }

    private static List<JsonObject> objects(JsonObject owner, String name) {
        List<JsonObject> found = new ArrayList<>();
        Optional<JsonArray> array = owner.array(name);
        if (array.isEmpty()) {
            return found;
        }
        for (JsonValue value : array.get().elements()) {
            if (value instanceof JsonObject object && found.size() < MAX_ENTRIES) {
                found.add(object);
            }
        }
        return found;
    }

    private static String text(JsonObject owner, String name) {
        return owner.string(name).orElse("");
    }

    private static long whole(JsonObject owner, String name) {
        return owner.number(name).map(JsonNumber::doubleValue).filter(Double::isFinite).map(Math::round).orElse(0L);
    }

    private static double real(JsonObject owner, String name) {
        return owner.number(name).map(JsonNumber::doubleValue).filter(Double::isFinite).orElse(0.0);
    }

    private static OptionalLong moment(JsonObject owner, String name) {
        Optional<JsonNumber> value = owner.number(name);
        return value.isPresent() && Double.isFinite(value.get().doubleValue())
                ? OptionalLong.of(Math.round(value.get().doubleValue())) : OptionalLong.empty();
    }
}
