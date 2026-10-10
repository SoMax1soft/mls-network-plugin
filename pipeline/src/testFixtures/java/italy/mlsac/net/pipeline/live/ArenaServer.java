// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.pipeline.compensation.ViewSeed;
import italy.mlsac.net.pipeline.engine.PipelinePorts;
import italy.mlsac.net.pipeline.environment.BackendAvailability;
import italy.mlsac.net.pipeline.environment.DataFolder;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.pipeline.environment.MonotonicClock;
import italy.mlsac.net.pipeline.environment.MonotonicPurpose;
import italy.mlsac.net.pipeline.environment.PlatformText;
import italy.mlsac.net.pipeline.environment.PlayerConnections;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.environment.TransactionPings;
import italy.mlsac.net.pipeline.environment.UltraWindowSink;
import italy.mlsac.net.pipeline.environment.UuidPurpose;
import italy.mlsac.net.pipeline.environment.UuidSource;
import italy.mlsac.net.pipeline.environment.WallClock;
import italy.mlsac.net.pipeline.environment.WallPurpose;
import italy.mlsac.net.pipeline.environment.WorldSightLines;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.player.PlayerPosture;
import italy.mlsac.net.pipeline.player.PlayerSample;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

final class ArenaServer implements PlayerStates, PlayerStanding, PlayerConnections, WorldSightLines, MonotonicClock,
        WallClock, UuidSource, DataFolder, PlatformText, DetectionSwitches, BackendAvailability, UltraWindowSink,
        TransactionPings {
    static final UUID WORLD = UUID.fromString("6d4c0e2a-91b3-4f57-8c2e-00000013aeaa");

    private final ThreadLocal<long[]> eventTime = ThreadLocal.withInitial(() -> new long[1]);
    private final List<UUID> connected = new CopyOnWriteArrayList<>();
    private final Map<UUID, PlayerSample> states = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> entityNumbers = new ConcurrentHashMap<>();
    private final Map<UUID, List<byte[]>> windows = new ConcurrentHashMap<>();
    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private final AtomicLong uuidCounter = new AtomicLong();

    PipelinePorts ports() {
        return new PipelinePorts(this, this, this, this, this, this, this, this, this, this, this, this,
                (player, window) -> windowsOf(player).add(window), this);
    }

    void at(long nanos) {
        eventTime.get()[0] = nanos;
    }

    void connect(UUID player, int entityNumber) {
        entityNumbers.put(player, entityNumber);
        connected.add(player);
    }

    void disconnect(UUID player) {
        connected.remove(player);
        states.remove(player);
    }

    void place(UUID player, long capturedAt, double x, double z, float yaw, float pitch) {
        states.put(player, new PlayerSample(player, WORLD, entityNumbers.get(player), capturedAt, x, 64.0, z, yaw,
                pitch, 1.62, 1.8, 0.6, 0.0, 1.0f, true, false, PlayerPosture.NEUTRAL, "diamond_sword", 12.5f,
                "SURVIVAL", null));
    }

    List<byte[]> windowsOf(UUID player) {
        return windows.computeIfAbsent(player, ignored -> Collections.synchronizedList(new ArrayList<>()));
    }

    Map<String, byte[]> files() {
        return files;
    }

    @Override
    public List<UUID> connectedPlayers() {
        return new ArrayList<>(connected);
    }

    @Override
    public PlayerSample sample(UUID player) {
        return states.get(player);
    }

    @Override
    public int serverPing(UUID player) {
        return 40;
    }

    @Override
    public boolean valid(UUID player) {
        return true;
    }

    @Override
    public boolean exempt(UUID player) {
        return false;
    }

    @Override
    public boolean bedrock(UUID player) {
        return false;
    }

    @Override
    public boolean zoneExcluded(UUID player) {
        return false;
    }

    @Override
    public boolean animating(UUID player) {
        return false;
    }

    @Override
    public String clientProtocol(UUID player) {
        return "V_1_21_4";
    }

    @Override
    public String clientLabel(UUID player) {
        return "1.21.4 (protocol 769)";
    }

    @Override
    public ViewSeed viewSeed(UUID player) {
        return null;
    }

    @Override
    public UUID playerByEntity(int entityNumber) {
        for (Map.Entry<UUID, Integer> entry : entityNumbers.entrySet()) {
            if (entry.getValue() == entityNumber) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public String name(UUID player) {
        return "Fighter" + entityNumbers.get(player);
    }

    @Override
    public RayVerdict trace(UUID world, Vec3 from, Vec3 to) {
        return RayVerdict.CLEAR;
    }

    @Override
    public long nanos(MonotonicPurpose purpose) {
        return eventTime.get()[0];
    }

    @Override
    public Instant instant(WallPurpose purpose) {
        return Instant.ofEpochMilli(1_789_000_000_000L);
    }

    @Override
    public String localStamp(WallPurpose purpose, String pattern) {
        return "20261001_120000";
    }

    @Override
    public UUID next(UuidPurpose purpose) {
        return new UUID(0L, uuidCounter.incrementAndGet());
    }

    @Override
    public boolean exists(String relativePath) {
        return false;
    }

    @Override
    public void write(String relativePath, byte[] content) {
        files.put(relativePath, content);
    }

    @Override
    public String lineSeparator() {
        return "\n";
    }

    @Override
    public Charset defaultCharset() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public boolean detectionEnabled() {
        return true;
    }

    @Override
    public boolean ultraEnabled() {
        return true;
    }

    @Override
    public boolean clientPresent() {
        return true;
    }

    @Override
    public boolean usable() {
        return true;
    }

    @Override
    public void accept(UUID player, byte[] window) {
        windowsOf(player).add(window);
    }

    @Override
    public void send(UUID viewer, int transactionId) {
    }
}
