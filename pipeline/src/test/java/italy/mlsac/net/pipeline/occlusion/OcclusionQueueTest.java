// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
