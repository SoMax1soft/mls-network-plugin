// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

public final class LiveOcclusion implements OcclusionPolicy {
    private final OcclusionProbe probe;
    private final ExposureMemory memory = new ExposureMemory();

    public LiveOcclusion(OcclusionProbe probe) {
        this.probe = probe;
    }

    @Override
    public void resolveTick(WallCheckRequest job) {
        if (memory.reusable(job)) {
            job.cell().exposure(memory.reuse());
            return;
        }
        double exposure = probe.exposure(job);
        memory.remember(job, exposure);
        job.cell().exposure(exposure);
    }

    @Override
    public void resolveClick(WallCheckRequest job) {
        double margin = probe.pickSlack(job);
        job.cell().wallMargin(margin);
        job.cell().rayOccluded(MarginVerdict.of(margin));
    }
}
