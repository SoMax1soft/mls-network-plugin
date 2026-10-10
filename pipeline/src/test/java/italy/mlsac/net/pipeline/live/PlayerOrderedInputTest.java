// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.engine.DataPipeline;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PlayerOrderedInputTest {
    private static final int FIGHTERS = 24;
    private static final int WORKERS = 6;
    private static final long TICKS = 360;
    private static final long RACE_TICKS = 2000;

    @Test
    void parallelPacketThreadsGiveEveryPlayerTheSameWindowsAsOneThread() throws Exception {
        Outcome sequential = play(1);
        Outcome parallel = play(WORKERS);
        assertEquals(sequential.windows().keySet(), parallel.windows().keySet());
        int produced = 0;
        for (Map.Entry<UUID, List<byte[]>> expected : sequential.windows().entrySet()) {
            List<byte[]> actual = parallel.windows().get(expected.getKey());
            assertEquals(expected.getValue().size(), actual.size(), "window count of " + expected.getKey());
            for (int index = 0; index < actual.size(); index++) {
                assertArrayEquals(expected.getValue().get(index), actual.get(index),
                        "window " + index + " of " + expected.getKey());
            }
            produced += actual.size();
        }
        assertTrue(produced >= FIGHTERS, "the fight should produce windows for the players, got " + produced);
    }

    @Test
    void ticksRacingPacketThreadsNeverDeadlockAndLeaveNoTurnsBehind() {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            ArenaServer server = new ArenaServer();
            ArenaFight fight = new ArenaFight(server, FIGHTERS);
            DataPipeline engine = new DataPipeline(server.ports());
            PlayerOrderedInput input = new PlayerOrderedInput(engine);
            input.enabled();
            AtomicLong liveTick = new AtomicLong(1);
            AtomicBoolean running = new AtomicBoolean(true);
            ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
            ExecutorService workers = Executors.newFixedThreadPool(WORKERS + 1);
            List<Future<?>> loops = new ArrayList<>();
            for (int worker = 0; worker < WORKERS; worker++) {
                int lane = worker;
                loops.add(workers.submit(() -> {
                    while (running.get()) {
                        try {
                            for (int index = lane; index < FIGHTERS; index += WORKERS) {
                                fight.tickClient(input, index, liveTick.get());
                            }
                        } catch (Throwable failure) {
                            failures.add(failure);
                            return;
                        }
                    }
                }));
            }
            loops.add(workers.submit(() -> {
                while (running.get()) {
                    try {
                        input.teleported(fight.players().get(2));
                        input.inLegacyCombat(fight.players().get(3));
                        Thread.sleep(5);
                    } catch (Throwable failure) {
                        failures.add(failure);
                        return;
                    }
                }
            }));
            for (long tick = 1; tick <= RACE_TICKS; tick++) {
                liveTick.set(tick);
                fight.tickServer(input, tick);
                if (tick % 60 == 0) {
                    UUID leaving = fight.players().get((int) (tick / 60) % FIGHTERS);
                    input.quit(leaving);
                    input.joined(leaving);
                }
            }
            running.set(false);
            for (Future<?> loop : loops) {
                loop.get(30, TimeUnit.SECONDS);
            }
            workers.shutdownNow();
            assertTrue(failures.isEmpty(), () -> "failures: " + failures);
            int produced = 0;
            for (UUID player : fight.players()) {
                produced += server.windowsOf(player).size();
            }
            assertTrue(produced > 0, "the racing fight should still produce windows");
            for (UUID player : fight.players()) {
                server.disconnect(player);
                input.quit(player);
            }
            assertEquals(0, engine.turns().held());
        });
    }

    private static Outcome play(int workerCount) throws Exception {
        ArenaServer server = new ArenaServer();
        ArenaFight fight = new ArenaFight(server, FIGHTERS);
        PlayerOrderedInput input = new PlayerOrderedInput(new DataPipeline(server.ports()));
        input.enabled();
        server.at(0L);
        ExecutorService workers = Executors.newFixedThreadPool(workerCount);
        try {
            for (long tick = 1; tick <= TICKS; tick++) {
                fight.tickServer(input, tick);
                long clientTick = tick;
                List<Future<?>> lanes = new ArrayList<>();
                for (int worker = 0; worker < workerCount; worker++) {
                    int lane = worker;
                    lanes.add(workers.submit(() -> {
                        for (int index = lane; index < FIGHTERS; index += workerCount) {
                            fight.tickClient(input, index, clientTick);
                        }
                    }));
                }
                for (Future<?> laneDone : lanes) {
                    laneDone.get(30, TimeUnit.SECONDS);
                }
            }
        } finally {
            workers.shutdownNow();
        }
        server.at((TICKS + 1) * ArenaFight.TICK_NANOS);
        Map<UUID, List<byte[]>> windows = new HashMap<>();
        for (UUID player : fight.players()) {
            windows.put(player, new ArrayList<>(server.windowsOf(player)));
        }
        return new Outcome(windows);
    }

    private record Outcome(Map<UUID, List<byte[]>> windows) {
    }
}
