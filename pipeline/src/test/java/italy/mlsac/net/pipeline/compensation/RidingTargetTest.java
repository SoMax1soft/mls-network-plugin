// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.geometry.LookPair;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.player.PlayerPosture;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.pipeline.player.ServerSamples;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RidingTargetTest {
    private static final UUID WORLD = new UUID(9, 9);
    private static final UUID VIEWER = new UUID(1, 1);
    private static final UUID TARGET = new UUID(2, 2);
    private static final int TARGET_ENTITY = 42;
    private static final long MS = TimeUnit.MILLISECONDS.toNanos(1);

    private final long[] now = {1_000 * MS};
    private final List<Integer> probes = new ArrayList<>();
    private final CompensationBook book = new CompensationBook((viewer, id) -> probes.add(id), purpose -> now[0]);
    private final ServerSamples samples = new ServerSamples();
    private final ClientSightRewind lens = new ClientSightRewind(book, samples);

    private static PlayerSample sample(UUID who, int entity, long at, double x, boolean riding) {
        PlayerPosture posture = new PlayerPosture(false, false, false, false, false, riding);
        return new PlayerSample(who, WORLD, entity, at, x, 64.0, 0.0, 0f, 0f, 1.62, 1.8, 0.6, 0.0, 1f, true, false,
                posture, "DIAMOND_SWORD", 12.5f, "SURVIVAL", 3.0);
    }

    private void acknowledgedViewerWithStaleTarget() {
        book.track(VIEWER, new ViewSeed("V_1_21_4", WORLD, new Vec3(0, 64, 0),
                List.of(new ViewSeed.SeenPlayer(TARGET, TARGET_ENTITY, WORLD, new Vec3(3, 64, 0)))));
        book.endViewerTick(VIEWER);
        int id = probes.get(0);
        book.pingWritten(VIEWER, id);
        now[0] += 50 * MS;
        assertTrue(book.pong(VIEWER, id));
        for (long t = now[0] - 400 * MS; t <= now[0]; t += 50 * MS) {
            samples.capture(sample(TARGET, TARGET_ENTITY, t, 10.0, true));
        }
    }

    private LensReading read(boolean riding) {
        PlayerSample eye = sample(VIEWER, 1, now[0], 7.5, false);
        PlayerSample target = sample(TARGET, TARGET_ENTITY, now[0], 10.0, riding);
        return lens.measure(VIEWER, eye, target, now[0], new LookPair(-90, 0, -90, 0)).lens();
    }

    @Test
    void riderIsMeasuredWhereTheServerHasHim() {
        acknowledgedViewerWithStaleTarget();
        LensReading reading = read(true);
        assertEquals(LensMode.REWIND, reading.mode());
        assertEquals(10.0, (reading.tight().low().x() + reading.tight().high().x()) / 2.0, 1e-9);
    }

    @Test
    void walkingTargetKeepsTheAcknowledgedBox() {
        acknowledgedViewerWithStaleTarget();
        LensReading reading = read(false);
        assertEquals(LensMode.TRANSACTIONS, reading.mode());
        assertEquals(3.0, (reading.tight().low().x() + reading.tight().high().x()) / 2.0, 1e-9);
    }
}
