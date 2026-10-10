// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import italy.mlsac.net.pipeline.compensation.ViewSeed;
import italy.mlsac.net.pipeline.environment.BackendAvailability;
import italy.mlsac.net.pipeline.environment.WorldSightLines;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.pipeline.environment.MonotonicClock;
import italy.mlsac.net.pipeline.environment.MonotonicPurpose;
import italy.mlsac.net.pipeline.environment.PlatformText;
import italy.mlsac.net.pipeline.environment.PlayerConnections;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.environment.UuidPurpose;
import italy.mlsac.net.pipeline.environment.UuidSource;
import italy.mlsac.net.pipeline.environment.WallClock;
import italy.mlsac.net.pipeline.environment.WallPurpose;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.json.JsonNumber;
import italy.mlsac.net.pipeline.player.PlayerPosture;
import italy.mlsac.net.pipeline.player.PlayerSample;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ReplayReads implements PlayerStates, PlayerStanding, PlayerConnections, WorldSightLines, MonotonicClock,
        WallClock, UuidSource, PlatformText, DetectionSwitches, BackendAvailability {
    private final TraceFile trace;
    private final ReplayMisses misses;
    private final ArrayDeque<ReplayScope> scopes = new ArrayDeque<>();
    private final Map<String, TraceRecord> latest = new HashMap<>();
    private final Map<RayKey, RayVerdict> rays = new HashMap<>();
    private final Map<Long, String> initCalls = new HashMap<>();
    private boolean backendUsable;
    private boolean backendPresent;

    ReplayReads(TraceFile trace, ReplayMisses misses) {
        this.trace = trace;
        this.misses = misses;
    }

    static String keyOf(TraceRecord read) {
        String name = read.name();
        if (read.has("p")) {
            return name + "|" + read.text("p");
        }
        if (read.has("e")) {
            return name + "|" + read.integer("e");
        }
        if (read.has("tag")) {
            return name + "|" + read.text("tag");
        }
        if (read.has("path")) {
            return name + "|" + read.text("path");
        }
        return name;
    }

    void enter(TraceEvent event) {
        for (TraceRecord read : event.allReads()) {
            if ("world.ray".equals(read.name())) {
                rays.put(rayKey(read), verdict(read.value()));
            }
        }
        scopes.push(new ReplayScope(event));
    }

    void leave() {
        ReplayScope scope = scopes.pop();
        for (TraceRecord read : scope.reads()) {
            latest.put(keyOf(read), read);
        }
        observeBackend(scope.event());
    }

    TraceEvent currentEvent() {
        return scopes.peek().event();
    }

    private void observeBackend(TraceEvent event) {
        for (TraceRecord record : event.records()) {
            if (record.isOutput() && "http".equals(record.name()) && record.text("url").endsWith("/api/v1/init")) {
                initCalls.put(record.integer("co"), record.text("url"));
            }
            if (record.isEvent() && "task".equals(record.name()) && "client.init".equals(record.text("site"))) {
                backendPresent = true;
            }
        }
        if ("http.done".equals(event.name()) && initCalls.containsKey(event.head().integer("co"))
                && event.head().has("code") && event.head().integer("code") == 200) {
            backendUsable = true;
        }
        if ("task".equals(event.name()) && "client.shutdown".equals(event.head().text("site"))) {
            backendUsable = false;
            backendPresent = false;
        }
        if ("plugin.config".equals(event.name()) && "reload".equals(event.head().text("why"))) {
            backendUsable = false;
        }
    }

    private TraceRecord flexible(String key) {
        ReplayScope scope = scopes.peek();
        TraceRecord read = scope.next(key);
        if (read != null) {
            return read;
        }
        read = scope.lastConsumed(key);
        if (read != null) {
            return read;
        }
        read = latest.get(key);
        if (read == null) {
            misses.note("unanswerable read", key + " in " + scope.event().head());
        }
        return read;
    }

    private TraceRecord strict(String key) {
        ReplayScope scope = scopes.peek();
        TraceRecord read = scope.next(key);
        if (read != null) {
            return read;
        }
        misses.note("read not recorded in this event", key + " in " + scope.event().head());
        read = scope.lastConsumed(key);
        return read != null ? read : latest.get(key);
    }

    private boolean flag(String name, UUID player) {
        TraceRecord read = flexible(name + "|" + player);
        return read != null && Boolean.TRUE.equals(read.value());
    }

    @Override
    public List<UUID> connectedPlayers() {
        TraceRecord read = flexible("server.players");
        List<UUID> players = new ArrayList<>();
        if (read != null) {
            for (Object value : (List<?>) read.value()) {
                players.add(UUID.fromString((String) value));
            }
        }
        return players;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PlayerSample sample(UUID player) {
        TraceRecord read = flexible("player.state|" + player);
        if (read == null || read.value() == null) {
            return null;
        }
        Map<String, Object> state = (Map<String, Object>) read.value();
        return new PlayerSample(player, UUID.fromString((String) state.get("w")),
                ((JsonNumber) state.get("e")).asInt(), ((JsonNumber) state.get("t")).asLong(),
                TraceValues.real(state.get("x")), TraceValues.real(state.get("y")), TraceValues.real(state.get("z")),
                TraceValues.single(state.get("yaw")), TraceValues.single(state.get("pitch")),
                TraceValues.real(state.get("eye")), TraceValues.real(state.get("height")),
                TraceValues.real(state.get("width")), TraceValues.real(state.get("fall")),
                TraceValues.single(state.get("cooldown")), Boolean.TRUE.equals(state.get("valid")),
                Boolean.TRUE.equals(state.get("dead")),
                new PlayerPosture(Boolean.TRUE.equals(state.get("sprinting")),
                        Boolean.TRUE.equals(state.get("sneaking")), Boolean.TRUE.equals(state.get("climbing")),
                        Boolean.TRUE.equals(state.get("in_water")), Boolean.TRUE.equals(state.get("blind")),
                        Boolean.TRUE.equals(state.get("riding"))),
                (String) state.get("item"), TraceValues.optionalSingle(state.get("period")),
                (String) state.get("mode"), TraceValues.optionalReal(state.get("reach")));
    }

    @Override
    public int serverPing(UUID player) {
        TraceRecord read = flexible("player.ping|" + player);
        return read == null ? -1 : ((JsonNumber) read.value()).asInt();
    }

    @Override
    public boolean valid(UUID player) {
        return flag("player.valid", player);
    }

    @Override
    public boolean exempt(UUID player) {
        return flag("player.exempt", player);
    }

    @Override
    public boolean bedrock(UUID player) {
        return flag("player.bedrock", player);
    }

    @Override
    public boolean zoneExcluded(UUID player) {
        return flag("player.region_exempt", player);
    }

    @Override
    public boolean animating(UUID player) {
        return flag("player.animating", player);
    }

    @Override
    public String clientProtocol(UUID player) {
        TraceRecord read = flexible("player.protocol|" + player);
        return read == null ? null : (String) read.value();
    }

    @Override
    public String clientLabel(UUID player) {
        TraceRecord read = flexible("player.client|" + player);
        return read == null ? "unknown" : (String) read.value();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ViewSeed viewSeed(UUID player) {
        TraceRecord read = flexible("player.view_seed|" + player);
        if (read == null || read.value() == null) {
            return null;
        }
        Map<String, Object> seed = (Map<String, Object>) read.value();
        if (!seed.containsKey("w")) {
            return new ViewSeed((String) seed.get("cv"), null, null, List.of());
        }
        List<ViewSeed.SeenPlayer> others = new ArrayList<>();
        for (Object entry : (List<Object>) seed.get("others")) {
            List<Object> fields = (List<Object>) entry;
            others.add(new ViewSeed.SeenPlayer(UUID.fromString((String) fields.get(0)),
                    ((JsonNumber) fields.get(1)).asInt(), UUID.fromString((String) fields.get(2)),
                    new Vec3(TraceValues.real(fields.get(3)), TraceValues.real(fields.get(4)),
                            TraceValues.real(fields.get(5)))));
        }
        return new ViewSeed((String) seed.get("cv"), UUID.fromString((String) seed.get("w")),
                new Vec3(TraceValues.real(seed.get("x")), TraceValues.real(seed.get("y")),
                        TraceValues.real(seed.get("z"))), others);
    }

    @Override
    public UUID playerByEntity(int entityNumber) {
        TraceRecord read = flexible("server.player_by_entity|" + entityNumber);
        return read == null || read.value() == null ? null : UUID.fromString((String) read.value());
    }

    @Override
    public String name(UUID player) {
        return trace.names().get(player);
    }

    @Override
    public RayVerdict trace(UUID world, Vec3 from, Vec3 to) {
        RayVerdict verdict = rays.get(RayKey.of(world, from, to));
        if (verdict == null) {
            misses.note("ray not recorded", world + " " + from + " -> " + to + " in " + scopes.peek().event().head());
            return RayVerdict.UNKNOWN;
        }
        return verdict;
    }

    @Override
    public long nanos(MonotonicPurpose purpose) {
        String key = "clock.nanos|" + purpose.tag();
        TraceRecord read = purpose == MonotonicPurpose.STREAM_GATE ? flexible(key) : strict(key);
        return read == null ? 0L : ((JsonNumber) read.value()).asLong();
    }

    @Override
    public Instant instant(WallPurpose purpose) {
        TraceRecord read = strict("clock.instant|" + purpose.tag());
        if (read == null) {
            return Instant.EPOCH;
        }
        List<?> parts = (List<?>) read.value();
        return Instant.ofEpochSecond(((JsonNumber) parts.get(0)).asLong(), ((JsonNumber) parts.get(1)).asLong());
    }

    @Override
    public String localStamp(WallPurpose purpose, String pattern) {
        TraceRecord read = strict("clock.local|" + purpose.tag());
        return read == null ? "" : (String) read.value();
    }

    @Override
    public UUID next(UuidPurpose purpose) {
        TraceRecord read = strict("random.uuid|" + purpose.tag());
        return read == null ? new UUID(0, 0) : UUID.fromString((String) read.value());
    }

    boolean fileExists(String path) {
        TraceRecord read = strict("file.exists|" + path);
        return read != null && Boolean.TRUE.equals(read.value());
    }

    @Override
    public String lineSeparator() {
        return trace.header().text("line_separator");
    }

    @Override
    public Charset defaultCharset() {
        return Charset.forName(trace.header().text("default_charset"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean detectionEnabled() {
        TraceRecord config = latestConfig();
        return config != null && Boolean.TRUE.equals(((Map<String, Object>) config.value()).get("detection.enabled"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean ultraEnabled() {
        TraceRecord config = latestConfig();
        if (config == null) {
            return false;
        }
        Object models = ((Map<String, Object>) config.value()).get("detection.models.*.enabled");
        return models instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("ultra"));
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> config() {
        TraceRecord config = latestConfig();
        return config == null ? Map.of() : (Map<String, Object>) config.value();
    }

    private TraceRecord latestConfig() {
        ReplayScope scope = scopes.peek();
        if (scope != null) {
            for (TraceRecord read : scope.reads()) {
                if ("config".equals(read.name())) {
                    return read;
                }
            }
        }
        return latest.get("config");
    }

    @Override
    public boolean clientPresent() {
        return backendPresent;
    }

    @Override
    public boolean usable() {
        return backendUsable;
    }

    private static RayKey rayKey(TraceRecord read) {
        List<Object> a = read.list("a");
        List<Object> b = read.list("b");
        return new RayKey(read.uuid("w"), TraceValues.real(a.get(0)), TraceValues.real(a.get(1)),
                TraceValues.real(a.get(2)), TraceValues.real(b.get(0)), TraceValues.real(b.get(1)),
                TraceValues.real(b.get(2)));
    }

    private static RayVerdict verdict(Object value) {
        if (Boolean.TRUE.equals(value)) {
            return RayVerdict.BLOCKED;
        }
        if (Boolean.FALSE.equals(value)) {
            return RayVerdict.CLEAR;
        }
        return RayVerdict.UNKNOWN;
    }
}
