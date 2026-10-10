// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import italy.mlsac.net.json.JsonArray;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonObjectBuilder;
import italy.mlsac.net.json.JsonString;
import italy.mlsac.net.json.JsonValue;
import italy.mlsac.net.json.JsonWriter;
import italy.mlsac.net.telemetry.ClientDistribution;
import italy.mlsac.net.telemetry.ServerEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BackendBodies {

    private static final String ALERT = "alert";
    private static final String PUNISH = "punish";
    private static final String SUPPRESS = "suppress";
    private static final String UNKNOWN_MODEL = "unknown";

    private final JsonWriter writer = JsonWriter.compact();

    public byte[] init(RequestIdentity identity, String accessKey, int online) {
        JsonObjectBuilder body = base(identity).put("apiKey", accessKey);
        return write(withOnline(body, online));
    }

    public byte[] init(RequestIdentity identity, String accessKey, int online, int ultraWire) {
        return init(identity, accessKey, online, ultraWire, ServerEnvironment.UNKNOWN);
    }

    public byte[] init(RequestIdentity identity, String accessKey, int online, int ultraWire,
                       ServerEnvironment environment) {
        JsonObjectBuilder body = base(identity).put("apiKey", accessKey);
        withOnline(body, online).put("ultraWire", (long) ultraWire);
        return write(TelemetryFields.appendEnvironment(body, environment));
    }

    public byte[] status(RequestIdentity identity, Optional<String> session, int online) {
        return status(identity, session, online, ClientDistribution.EMPTY);
    }

    public byte[] status(RequestIdentity identity, Optional<String> session, int online, ClientDistribution clients) {
        JsonObjectBuilder body = withOnline(withSession(base(identity), session), online);
        return write(TelemetryFields.appendClients(body, clients));
    }

    public byte[] poll(RequestIdentity identity, Optional<String> session) {
        return write(withSession(base(identity), session));
    }

    public byte[] alert(RequestIdentity identity, Optional<String> session, AlertReport report) {
        Incident incident = new Incident(ALERT, report.player().toString(), report.playerNick(), report.model(),
                report.probability(), report.buffer(), 0L, ALERT, "");
        return write(incident.appendTo(withSession(base(identity), session)));
    }

    public byte[] punish(RequestIdentity identity, Optional<String> session, PunishReport report) {
        Incident incident = new Incident(PUNISH, report.player().toString(), report.playerNick(), UNKNOWN_MODEL,
                report.probability(), report.buffer(), report.offenceLevel(), report.action(), report.command());
        return write(incident.appendTo(withSession(base(identity), session)));
    }

    public byte[] suppression(RequestIdentity identity, Optional<String> session, SuppressionReport report) {
        List<JsonValue> actions = new ArrayList<>(report.actions().size());
        for (String action : report.actions()) {
            actions.add(new JsonString(action));
        }
        JsonObjectBuilder body = withSession(base(identity), session)
                .put("type", SUPPRESS)
                .put("playerUuid", report.player().toString())
                .put("playerId", report.player().toString())
                .put("playerName", report.playerNick())
                .put("probability", report.probability())
                .put("buffer", report.buffer())
                .put("trigger", report.trigger().wireName())
                .put("level", (long) report.level())
                .put("actions", new JsonArray(actions));
        return write(body);
    }

    public byte[] predict(RequestIdentity identity, Optional<String> session, WindowSubmission request) {
        JsonObjectBuilder body = withSession(base(identity), session)
                .put("playerData", request.windowBase64())
                .put("playerUuid", request.player().toString())
                .put("playerName", request.playerNick());
        return write(body);
    }

    public byte[] predictScrambled(RequestIdentity identity, Optional<String> session, WindowSubmission request,
                                   int wireVersion, JsonArray data) {
        JsonObjectBuilder body = withSession(base(identity), session)
                .put("wire_version", (long) wireVersion)
                .put("data", data)
                .put("playerUuid", request.player().toString())
                .put("playerName", request.playerNick());
        return write(body);
    }

    public byte[] emptyObject() {
        return write(JsonObject.builder());
    }

    private static JsonObjectBuilder base(RequestIdentity identity) {
        JsonObjectBuilder body = JsonObject.builder()
                .put("pluginVersion", identity.buildVersion())
                .put("pluginGeneration", (long) RequestIdentity.GENERATION_MARK)
                .put("interserverEventsSupported", true)
                .put("serverName", identity.hostLabel())
                .put("serverFamily", identity.platformKind())
                .put("family", identity.platformKind())
                .put("serverIp", identity.serverIp())
                .put("serverPort", (long) identity.serverPort())
                .put("interServer", identity.interServer());
        identity.preset().ifPresent(name -> body.put("preset", name));
        return body;
    }

    private static JsonObjectBuilder withSession(JsonObjectBuilder body, Optional<String> session) {
        session.ifPresent(id -> body.put("sessionId", id));
        return body;
    }

    private static JsonObjectBuilder withOnline(JsonObjectBuilder body, int online) {
        return body.put("onlinePlayers", (long) online).put("onlineCount", (long) online);
    }

    private byte[] write(JsonObjectBuilder body) {
        return writer.writeUtf8(body.build());
    }

    private record Incident(String type, String playerUuid, String nick, String model, double probability,
            double buffer, long level, String action, String command) {

        JsonObjectBuilder appendTo(JsonObjectBuilder body) {
            return body.put("type", type)
                    .put("playerUuid", playerUuid)
                    .put("playerId", playerUuid)
                    .put("playerName", nick)
                    .put("model", model)
                    .put("probability", probability)
                    .put("buffer", buffer)
                    .put("vl", level)
                    .put("violationLevel", level)
                    .put("action", action)
                    .put("command", command);
        }
    }
}
