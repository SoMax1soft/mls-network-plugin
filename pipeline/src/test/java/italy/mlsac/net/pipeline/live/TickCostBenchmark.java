// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.compensation.ViewSeed;
import italy.mlsac.net.pipeline.engine.DataPipeline;
import italy.mlsac.net.pipeline.engine.PipelinePorts;
import italy.mlsac.net.pipeline.environment.DetectionSwitches;
import italy.mlsac.net.pipeline.environment.PlayerConnections;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import italy.mlsac.net.pipeline.environment.PlayerStates;
import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.environment.TransactionPings;
import italy.mlsac.net.pipeline.environment.WorldSightLines;
import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.input.SwingPacket;
import italy.mlsac.net.pipeline.input.TickEndPacket;
import italy.mlsac.net.pipeline.player.PlayerSample;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class TickCostBenchmark {
    private static final int FIRST_ENTITY = 9000;
    private static final long TICK_NANOS = 50_000_000L;
    private static final int WARMUP_TICKS = 4000;
    private static final int MEASURED_TICKS = 2000;
    private static final int ROUNDS = 7;
    private static final int[] SWEEP = new int[48 * 1024 * 1024 / Integer.BYTES];
    private static long sink;

    @Test
    void outputsOfOneArenaRun() throws NoSuchAlgorithmException, IOException {
        Assumptions.assumeTrue(Boolean.getBoolean("mlsac.bench"), "benchmark runs with -Pbench=true");
        int players = Integer.getInteger("mlsac.bench.players", 126);
        StringBuilder out = new StringBuilder();
        for (boolean ultra : new boolean[] {true, false}) {
            Arena arena = new Arena(players, ultra);
            arena.play(1500, false, false);
            out.append(ultra ? "ultra " : "legacy").append(" windows ").append(arena.windowDigest())
                    .append(" pings ").append(arena.pingDigest()).append('\n');
        }
        System.out.println(out);
        Path report = Path.of(System.getProperty("mlsac.parity.folder", "build"), "..", "bench",
                "outputs-" + players + "-" + Integer.getInteger("mlsac.bench.walls", 20) + "hz.txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, out.toString(), StandardCharsets.UTF_8);
    }

    @Test
    void serverThreadCostPerTick() throws NoSuchAlgorithmException, IOException {
        Assumptions.assumeTrue(Boolean.getBoolean("mlsac.bench"), "benchmark runs with -Pbench=true");
        int players = Integer.getInteger("mlsac.bench.players", 126);
        Arena arena = new Arena(players, true);
        arena.play(WARMUP_TICKS, false, false);
        StringBuilder out = new StringBuilder();
        out.append(String.format(Locale.ROOT, "players=%d ticks=%d rounds=%d%n", players, MEASURED_TICKS, ROUNDS));
        Map<String, Long> calls = null;
        for (boolean cold : new boolean[] {false, true}) {
            long[] rounds = new long[ROUNDS];
            for (int round = 0; round < ROUNDS; round++) {
                arena.counts.clear();
                rounds[round] = arena.play(MEASURED_TICKS / (cold ? 4 : 1), true, cold) * (cold ? 4 : 1);
                calls = cold ? calls : arena.counts.snapshot();
            }
            Arrays.sort(rounds);
            double median = rounds[ROUNDS / 2] / (double) MEASURED_TICKS / 1e6;
            double best = rounds[0] / (double) MEASURED_TICKS / 1e6;
            out.append(String.format(Locale.ROOT,
                    "%s gameTick ms/tick: median %.4f best %.4f (%.2f%% of 50 ms), us/player %.3f%n",
                    cold ? "cold caches" : "hot caches ", median, best, median * 2, median * 1000 / players));
        }
        out.append("port calls per tick (server thread):\n");
        for (Map.Entry<String, Long> call : calls.entrySet()) {
            out.append(String.format(Locale.ROOT, "  %-28s %10.1f%n", call.getKey(),
                    call.getValue() / (double) MEASURED_TICKS));
        }
        out.append("windows ").append(arena.windowDigest()).append('\n');
        System.out.println(out);
        Path report = Path.of(System.getProperty("mlsac.parity.folder", "build"), "..", "bench",
                "tick-cost-" + players + ".txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, out.toString(), StandardCharsets.UTF_8);
        assertTrue(arena.windows() > 0, "the arena produced no windows");
    }

    private static final class Counts {
        private final Map<String, LongAdder> adders = new java.util.concurrent.ConcurrentHashMap<>();
        private volatile boolean on;

        void hit(String name) {
            if (on) {
                adders.computeIfAbsent(name, ignored -> new LongAdder()).increment();
            }
        }

        void clear() {
            adders.clear();
        }

        Map<String, Long> snapshot() {
            Map<String, Long> copy = new TreeMap<>();
            adders.forEach((name, adder) -> copy.put(name, adder.sum()));
            return copy;
        }
    }

    private static final class Arena {
        private final ArenaServer server = new ArenaServer();
        private final Counts counts = new Counts();
        private final List<UUID> players = new ArrayList<>();
        private final List<int[]> pending = new ArrayList<>();
        private final PlayerOrderedInput input;
        private final MessageDigest pingDigest;
        private long tick;

        Arena(int count, boolean ultra) {
            try {
                pingDigest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
            for (int index = 0; index < count; index++) {
                UUID player = new UUID(0xBE7CL, index + 1L);
                players.add(player);
                server.connect(player, FIRST_ENTITY + index);
            }
            PipelinePorts base = server.ports();
            CountingPorts counted = new CountingPorts(base, counts, pending, players, ultra, pingDigest);
            PipelinePorts ports = new PipelinePorts(counted, counted, counted, counted, base.monotonic(),
                    base.wallClock(), base.uuids(), base.folder(), base.platform(), counted, base.backend(),
                    base.ultraWindows(), base.legacyWindows(), counted);
            input = new PlayerOrderedInput(new DataPipeline(ports));
            input.enabled();
        }

        long play(int ticks, boolean measured, boolean cold) {
            long spent = 0;
            for (int step = 0; step < ticks; step++, tick++) {
                for (int index = 0; index < players.size(); index++) {
                    double[] spot = spot(index, tick);
                    server.place(players.get(index), tick * TICK_NANOS, spot[0], spot[1], 0f, 0f);
                }
                server.at(tick * TICK_NANOS);
                if (cold) {
                    evictCaches();
                }
                counts.on = measured;
                long began = System.nanoTime();
                input.gameTick(tick);
                spent += System.nanoTime() - began;
                counts.on = false;
                acknowledgePings();
                for (int index = 0; index < players.size(); index++) {
                    clientTick(index);
                }
            }
            return spent;
        }

        private void evictCaches() {
            long sum = 0;
            for (int index = 0; index < SWEEP.length; index += 16) {
                SWEEP[index] += index;
                sum += SWEEP[index];
            }
            sink += sum;
        }

        private void acknowledgePings() {
            List<int[]> sent = new ArrayList<>(pending);
            pending.clear();
            for (int[] ping : sent) {
                UUID viewer = players.get(ping[0]);
                input.pingWritten(viewer, ping[1]);
                input.pong(viewer, ping[1]);
            }
        }

        private void clientTick(int index) {
            UUID player = players.get(index);
            Random jitter = new Random(index * 7_919L + tick * 31L);
            long base = tick * TICK_NANOS;
            double[] own = spot(index, tick);
            double[] other = spot(index ^ 1, tick);
            float yaw = (float) (Math.toDegrees(Math.atan2(-(other[0] - own[0]), other[1] - own[1]))
                    + jitter.nextGaussian() * 3.0);
            float pitch = (float) (8.0 + jitter.nextGaussian() * 2.0);
            if (jitter.nextInt(4) != 0) {
                long moveAt = base + 3_000_000L + index * 50_000L;
                server.at(moveAt);
                input.movement(new MovementPacket(player, moveAt, yaw, pitch, true, own[0], 64.0, own[1], true, true,
                        false));
            }
            if ((tick + index) % 6 == 0 && index % 3 != 2) {
                long swingAt = base + 20_000_000L;
                server.at(swingAt);
                input.swing(new SwingPacket(player, swingAt, true));
                input.attack(new AttackPacket(player, swingAt, FIRST_ENTITY + (index ^ 1), false));
            }
            if (jitter.nextInt(40) == 0) {
                server.at(base + 30_000_000L);
                input.slotChanged(player);
            }
            long endAt = base + 45_000_000L;
            server.at(endAt);
            input.clientTickEnd(new TickEndPacket(player, endAt, "V_1_21_4"));
        }

        int windows() {
            int total = 0;
            for (UUID player : players) {
                total += server.windowsOf(player).size();
            }
            return total;
        }

        String pingDigest() {
            return HexFormat.of().formatHex(pingDigest.digest());
        }

        void eachWindow(String mode, StringBuilder out) throws NoSuchAlgorithmException {
            for (UUID player : players) {
                int index = 0;
                for (byte[] window : server.windowsOf(player)) {
                    out.append(mode).append(' ').append(player).append(' ').append(index++).append(' ')
                            .append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(window)))
                            .append('\n');
                }
            }
        }

        String windowDigest() throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int total = 0;
            for (UUID player : players) {
                synchronized (server.windowsOf(player)) {
                    for (byte[] window : server.windowsOf(player)) {
                        digest.update(window);
                        total++;
                    }
                }
            }
            return total + ":" + HexFormat.of().formatHex(digest.digest());
        }

        private static double[] spot(int index, long tick) {
            int pair = index / 2;
            double angle = tick * 0.05 + pair + (index % 2 == 0 ? 0.0 : Math.PI);
            return new double[] {(pair % 8) * 40.0 + 1.4 * Math.cos(angle), (pair / 8) * 40.0 + 1.4 * Math.sin(angle)};
        }
    }

    private static final class CountingPorts implements PlayerStates, PlayerStanding, PlayerConnections,
            WorldSightLines, DetectionSwitches, TransactionPings {
        private final PipelinePorts base;
        private final Counts counts;
        private final List<int[]> pending;
        private final Map<UUID, Integer> indexes = new java.util.HashMap<>();
        private final boolean ultra;
        private final MessageDigest pings;
        private final Map<UUID, ModelEntity> online = new java.util.HashMap<>();
        private final Map<UUID, ModelEntity> standings = new java.util.concurrent.ConcurrentHashMap<>();
        private final List<Object> scatter = new ArrayList<>();
        private static long touched;

        CountingPorts(PipelinePorts base, Counts counts, List<int[]> pending, List<UUID> players, boolean ultra,
                MessageDigest pings) {
            this.ultra = ultra;
            this.pings = pings;
            this.base = base;
            this.counts = counts;
            this.pending = pending;
            for (int index = 0; index < players.size(); index++) {
                indexes.put(players.get(index), index);
                online.put(players.get(index), new ModelEntity(index, scatter));
                standings.put(players.get(index), new ModelEntity(index, scatter));
            }
        }

        private void serverLookup(UUID player) {
            ModelEntity entity = online.get(player);
            touched += entity == null ? 0 : entity.read();
        }

        private void standingLookup(UUID player) {
            ModelEntity entity = standings.get(player);
            touched += entity == null ? 0 : entity.flags;
        }

        private <T> T count(String name, Supplier<T> call) {
            counts.hit(name);
            return call.get();
        }

        @Override
        public List<UUID> connectedPlayers() {
            return count("states.connectedPlayers", base.states()::connectedPlayers);
        }

        @Override
        public PlayerSample sample(UUID player) {
            serverLookup(player);
            serverLookup(player);
            return count("states.sample", () -> base.states().sample(player));
        }

        @Override
        public int serverPing(UUID player) {
            serverLookup(player);
            return count("states.serverPing", () -> base.states().serverPing(player));
        }

        public int serverPingAfterSample(UUID player) {
            return count("states.serverPingAfterSample", () -> base.states().serverPing(player));
        }

        @Override
        public boolean valid(UUID player) {
            serverLookup(player);
            serverLookup(player);
            return count("standing.valid", () -> base.standing().valid(player));
        }

        @Override
        public boolean exempt(UUID player) {
            standingLookup(player);
            return count("standing.exempt", () -> base.standing().exempt(player));
        }

        @Override
        public boolean bedrock(UUID player) {
            standingLookup(player);
            return count("standing.bedrock", () -> base.standing().bedrock(player));
        }

        @Override
        public boolean zoneExcluded(UUID player) {
            standingLookup(player);
            return count("standing.zoneExcluded", () -> base.standing().zoneExcluded(player));
        }

        @Override
        public boolean animating(UUID player) {
            return count("standing.animating", () -> base.standing().animating(player));
        }

        @Override
        public String clientProtocol(UUID player) {
            return base.connections().clientProtocol(player);
        }

        @Override
        public String clientLabel(UUID player) {
            return base.connections().clientLabel(player);
        }

        @Override
        public ViewSeed viewSeed(UUID player) {
            return new ViewSeed("V_1_21_4", ArenaServer.WORLD, new Vec3(0, 64, 0), List.of());
        }

        @Override
        public UUID playerByEntity(int entityNumber) {
            return base.connections().playerByEntity(entityNumber);
        }

        @Override
        public String name(UUID player) {
            return base.connections().name(player);
        }

        @Override
        public RayVerdict trace(UUID world, Vec3 from, Vec3 to) {
            counts.hit("rays.trace");
            return RayVerdict.CLEAR;
        }

        @Override
        public boolean provenClear(UUID world, Vec3 from, Box ends) {
            counts.hit("rays.provenClear");
            return ((int) Math.floor(from.x() * 4.0) & 1) == 0;
        }

        @Override
        public boolean detectionEnabled() {
            return base.switches().detectionEnabled();
        }

        @Override
        public boolean ultraEnabled() {
            return count("switches.ultraEnabled", () -> ultra);
        }

        @Override
        public boolean ultraEnabledFor(UUID player) {
            return count("switches.ultraEnabledFor", () -> ultra);
        }

        @Override
        public int wallChecksPerSecond() {
            return Integer.getInteger("mlsac.bench.walls", 20);
        }

        @Override
        public boolean legacyEnabledFor(UUID player) {
            return count("switches.legacyEnabledFor", () -> !ultra);
        }

        @Override
        public void send(UUID viewer, int transactionId) {
            counts.hit("pings.send");
            pending.add(new int[] {indexes.get(viewer), transactionId});
            pings.update((indexes.get(viewer) + ":" + transactionId + ";").getBytes(StandardCharsets.US_ASCII));
        }

        @Override
        public void openBurst() {
            counts.hit("pings.openBurst");
        }

        @Override
        public void closeBurst() {
            counts.hit("pings.closeBurst");
        }
    }

    private static final class ModelEntity {
        private final int[] location;
        private final int[] inventory;
        private final Map<Integer, Integer> effects = new java.util.HashMap<>();
        private final int flags;

        ModelEntity(int seed, List<Object> scatter) {
            scatter.add(new byte[4096 + seed * 64]);
            location = new int[] {seed, seed + 1, seed + 2};
            scatter.add(new byte[2048 + seed * 32]);
            inventory = new int[] {seed * 3, seed * 5};
            scatter.add(new byte[1024]);
            effects.put(seed & 7, seed);
            flags = seed & 3;
        }

        long read() {
            Integer effect = effects.get(location[0] & 7);
            return location[1] + inventory[1] + (effect == null ? 0 : effect) + flags;
        }
    }
}
