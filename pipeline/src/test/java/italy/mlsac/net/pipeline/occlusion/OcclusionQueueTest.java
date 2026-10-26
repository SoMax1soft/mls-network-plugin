// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.environment.RayVerdict;
import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.player.PlayerPosture;
import italy.mlsac.net.pipeline.player.PlayerSample;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OcclusionQueueTest {
    private static final Box TIGHT = Box.player(0.0, 64.0, 3.0, 0.6, 1.8);
    private static final PlayerSample EYE = new PlayerSample(new UUID(1, 1), new UUID(2, 2), 1, 0L, 0.0, 64.0, 0.0,
            0f, 0f, 1.62, 1.8, 0.6, 0.0, 1f, true, false, PlayerPosture.NEUTRAL, "air", null, null, null);

    @Test
    void keepsOnlyTheNewestFourHundredJobs() {
        OcclusionQueue queue = new OcclusionQueue(new MarginOcclusion(new OcclusionProbe((w, a, b) -> RayVerdict.CLEAR)));
        List<OcclusionCell> cells = new ArrayList<>();
        for (int index = 0; index < 401; index++) {
            OcclusionCell cell = new OcclusionCell();
            cells.add(cell);
            queue.enqueue(WallCheckRequest.of(OcclusionKind.TICK, cell, EYE, null, TIGHT, TIGHT, 0f, 0f));
        }
        queue.runAll();
        assertEquals(-1.0, cells.get(0).exposure());
        assertEquals(1.0, cells.get(1).exposure());
        assertEquals(0, cells.get(400).rayOccluded());
    }

    @Test
    void liveExposureIsReusedTwentyTimesThenRecomputed() {
        int[] rays = {0};
        LiveOcclusion live = new LiveOcclusion(new OcclusionProbe((w, a, b) -> {
            rays[0]++;
            return RayVerdict.BLOCKED;
        }));
        for (int index = 0; index < 22; index++) {
            live.resolveTick(WallCheckRequest.of(OcclusionKind.TICK, new OcclusionCell(), EYE, null, TIGHT, TIGHT, 0f,
                    0f));
        }
        assertEquals(18, rays[0]);
    }

    @Test
    void recordingClickGetsMarginVerdicts() {
        OcclusionCell cell = new OcclusionCell();
        new MarginOcclusion(new OcclusionProbe((w, a, b) -> RayVerdict.BLOCKED))
                .resolveClick(WallCheckRequest.of(OcclusionKind.CLICK, cell, EYE, null, TIGHT, TIGHT, 0f, 0f));
        assertEquals(1.0, cell.wallMargin());
        assertEquals(1, cell.rayOccluded());
        assertEquals(1, cell.centerOccluded());
        assertEquals(0.0, cell.exposure());
    }

    @Test
    void halfRateWallChecksReuseTheValueOfTheTickBefore() {
        int[] checks = {0};
        OcclusionProbe probe = new OcclusionProbe((w, a, b) -> {
            checks[0]++;
            return checks[0] % 3 == 0 ? RayVerdict.BLOCKED : RayVerdict.CLEAR;
        });
        LiveOcclusion half = new LiveOcclusion(probe, () -> 10);
        List<Double> seen = new ArrayList<>();
        List<Integer> casts = new ArrayList<>();
        for (long tick : new long[] {1, 2, 3, 4, 5, 6, 8, 9}) {
            OcclusionCell cell = new OcclusionCell();
            int before = checks[0];
            half.rowTick(tick);
            half.resolveTick(WallCheckRequest.of(OcclusionKind.TICK, cell, EYE, null,
                    TIGHT, Box.player(0.0, 64.0, 3.0 + tick * 0.01, 0.6, 1.8), 0f, 0f));
            seen.add(cell.exposure());
            casts.add(checks[0] - before);
        }
        assertEquals(seen.get(0), seen.get(1));
        assertEquals(seen.get(2), seen.get(3));
        assertEquals(seen.get(4), seen.get(5));
        assertEquals(0, casts.get(1));
        assertEquals(0, casts.get(3));
        assertEquals(0, casts.get(5));
        assertTrue(casts.get(0) > 0 && casts.get(2) > 0 && casts.get(4) > 0);
        assertTrue(casts.get(6) > 0, "a tick after a gap is checked");
        assertTrue(casts.get(7) > 0);
    }

    @Test
    void fullRateChecksEveryTick() {
        int[] checks = {0};
        LiveOcclusion full = new LiveOcclusion(new OcclusionProbe((w, a, b) -> {
            checks[0]++;
            return RayVerdict.BLOCKED;
        }));
        for (long tick = 1; tick <= 6; tick++) {
            full.rowTick(tick);
            full.resolveTick(WallCheckRequest.of(OcclusionKind.TICK, new OcclusionCell(), EYE, null,
                    TIGHT, Box.player(0.0, 64.0, 3.0 + tick * 0.01, 0.6, 1.8), 0f, 0f));
        }
        assertEquals(6 * 9, checks[0]);
    }
}
