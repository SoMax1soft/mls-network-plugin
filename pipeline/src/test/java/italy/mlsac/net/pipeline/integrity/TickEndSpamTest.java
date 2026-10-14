// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.integrity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.bus.SynchronousEventBus;
import italy.mlsac.net.config.integrity.BadPacketSettings;
import italy.mlsac.net.detection.BadPacketKind;
import italy.mlsac.net.detection.BadPacketObserved;
import italy.mlsac.net.packet.ClientTickEndPacket;
import italy.mlsac.net.pipeline.engine.PipelineInput;
import italy.mlsac.net.pipeline.environment.PlayerStanding;
import italy.mlsac.net.pipeline.live.PacketFeed;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.FakePacketLayer;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestLogs;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TickEndSpamTest {
    private static final long SECOND = 1_000_000_000L;
    private static final long TICK = SECOND / 20;

    private final UUID player = new UUID(9, 9);
    private final FakePacketLayer packets = new FakePacketLayer(GameVersion.parse("1.21.4").orElseThrow());
    private final SynchronousEventBus bus = new SynchronousEventBus(
            TestLogs.capturing(new CapturingLogSink(), new ManualClock(0), false));
    private final List<Long> reportedAt = new ArrayList<>();
    private final int[] reachedPipeline = {0};
    private boolean exempt;
    private int dropped;
    private long lastNanos;

    @Test
    void steadyClientWithJitterIsNeverTouched() {
        start(ClientTickPace.VANILLA);
        Random jitter = new Random(5);
        for (long tick = 0; tick < 20 * 600; tick++) {
            send(tick * TICK + 20_000_000L + (long) (jitter.nextGaussian() * 15_000_000L));
        }
        assertEquals(0, dropped);
        assertTrue(reportedAt.isEmpty());
        assertEquals(20 * 600, reachedPipeline[0]);
    }

    @Test
    void clientFreezeCatchUpBurstsAreNeverTouched() {
        start(ClientTickPace.VANILLA);
        long at = 0;
        for (int round = 0; round < 40; round++) {
            for (int tick = 0; tick < 100; tick++) {
                at += TICK;
                send(at);
            }
            at += 2 * SECOND;
            for (int catchUp = 0; catchUp < 10; catchUp++) {
                send(at);
            }
        }
        assertEquals(0, dropped);
        assertTrue(reportedAt.isEmpty());
    }

    @Test
    void networkStallOfNineSecondsDeliveredAtOnceIsNeverTouched() {
        start(ClientTickPace.VANILLA);
        long at = 0;
        for (int round = 0; round < 8; round++) {
            for (int tick = 0; tick < 600; tick++) {
                at += TICK;
                send(at);
            }
            at += 9 * SECOND;
            for (int queued = 0; queued < 180; queued++) {
                send(at + queued * 1_000L);
            }
        }
        assertEquals(0, dropped);
        assertTrue(reportedAt.isEmpty());
    }

    @Test
    void sustainedSixtyPerSecondIsDroppedAndReportedAtMostOncePerSecond() {
        start(ClientTickPace.VANILLA);
        long firstDrop = -1;
        for (long packet = 0; packet < 60 * 30; packet++) {
            int before = dropped;
            send(packet * SECOND / 60);
            if (firstDrop < 0 && dropped > before) {
                firstDrop = lastNanos;
            }
        }
        assertTrue(firstDrop > 6 * SECOND && firstDrop < 8 * SECOND, "first drop at " + firstDrop);
        assertTrue(dropped > 600 && dropped < 800, "dropped " + dropped);
        assertEquals(60 * 30 - dropped, reachedPipeline[0]);
        for (int index = 1; index < reportedAt.size(); index++) {
            assertTrue(reportedAt.get(index) - reportedAt.get(index - 1) >= SECOND);
        }
        long inLastMinute = reportedAt.stream().filter(at -> at > lastNanos - 60 * SECOND).count();
        assertTrue(inLastMinute >= 3, "reports " + reportedAt.size());
    }

    @Test
    void fortyPerSecondIsCaughtOnceTheBurstIsSpent() {
        start(ClientTickPace.VANILLA);
        for (long packet = 0; packet < 40 * 60; packet++) {
            send(packet * SECOND / 40);
        }
        assertTrue(reportedAt.size() >= 3, "reports " + reportedAt.size());
        assertTrue(reportedAt.get(0) > 19 * SECOND, "first report " + reportedAt.get(0));
    }

    @Test
    void serverTickRateRaisesTheBudget() {
        start(() -> 40.0);
        for (long packet = 0; packet < 40 * 120; packet++) {
            send(packet * SECOND / 40);
        }
        assertEquals(0, dropped);
    }

    @Test
    void exemptPlayersAreNeverTouched() {
        exempt = true;
        start(ClientTickPace.VANILLA);
        for (long packet = 0; packet < 100 * 30; packet++) {
            send(packet * SECOND / 100);
        }
        assertEquals(0, dropped);
        assertEquals(100 * 30, reachedPipeline[0]);
    }

    private void start(ClientTickPace pace) {
        bus.subscribe(BadPacketObserved.class, observed -> {
            assertEquals(BadPacketKind.TICK_END_SPAM, observed.kind());
            reportedAt.add(lastNanos);
        });
        new PacketIntegrityGuard(packets, bus, BadPacketSettings::pluginDefaults, standing(), pace, () -> 0L).start();
        packets.setClientVersion(player, GameVersion.parse("1.21.4").orElseThrow());
        new PacketFeed(packets, pipeline(), bus).start();
    }

    private void send(long nanos) {
        lastNanos = nanos;
        if (packets.receive(new ClientTickEndPacket(player, nanos))) {
            dropped++;
        }
    }

    private PlayerStanding standing() {
        return new PlayerStanding() {
            @Override
            public boolean valid(UUID id) {
                return true;
            }

            @Override
            public boolean exempt(UUID id) {
                return exempt;
            }

            @Override
            public boolean bedrock(UUID id) {
                return false;
            }

            @Override
            public boolean zoneExcluded(UUID id) {
                return false;
            }

            @Override
            public boolean animating(UUID id) {
                return false;
            }
        };
    }

    private PipelineInput pipeline() {
        return (PipelineInput) Proxy.newProxyInstance(PipelineInput.class.getClassLoader(),
                new Class<?>[] {PipelineInput.class}, (proxy, method, args) -> {
                    if (method.getName().equals("clientTickEnd")) {
                        reachedPipeline[0]++;
                    }
                    return method.getReturnType() == boolean.class ? false : null;
                });
    }
}
