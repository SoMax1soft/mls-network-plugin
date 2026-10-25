// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

import java.util.function.IntSupplier;

public final class LiveOcclusion implements OcclusionPolicy {
    private static final int HALF_RATE = 10;

    private final OcclusionProbe probe;
    private final IntSupplier checksPerSecond;
    private final ExposureMemory memory = new ExposureMemory();
    private long rowTick;
    private long lastCheckTick = Long.MIN_VALUE;
    private double lastExposure;

    public LiveOcclusion(OcclusionProbe probe) {
        this(probe, () -> 20);
    }

    public LiveOcclusion(OcclusionProbe probe, IntSupplier checksPerSecond) {
        this.probe = probe;
        this.checksPerSecond = checksPerSecond;
    }

    @Override
    public void rowTick(long tick) {
        rowTick = tick;
    }

    @Override
    public void resolveTick(WallCheckRequest job) {
        if (rowTick % 2 == 0 && lastCheckTick == rowTick - 1 && checksPerSecond.getAsInt() == HALF_RATE) {
            job.cell().exposure(lastExposure);
            return;
        }
        double exposure;
        if (memory.reusable(job)) {
            exposure = memory.reuse();
        } else {
            exposure = probe.exposure(job);
            memory.remember(job, exposure);
        }
        job.cell().exposure(exposure);
        lastExposure = exposure;
        lastCheckTick = rowTick;
    }

    @Override
    public void resolveClick(WallCheckRequest job) {
        double margin = probe.pickSlack(job);
        job.cell().wallMargin(margin);
        job.cell().rayOccluded(MarginVerdict.of(margin));
    }
}
