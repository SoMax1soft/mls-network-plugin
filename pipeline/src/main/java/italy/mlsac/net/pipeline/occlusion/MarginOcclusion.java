// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

public final class MarginOcclusion implements OcclusionPolicy {
    private final OcclusionProbe probe;

    public MarginOcclusion(OcclusionProbe probe) {
        this.probe = probe;
    }

    @Override
    public void resolveTick(WallCheckRequest job) {
        job.cell().rayOccluded(probe.crosshairVerdict(job));
        job.cell().centerOccluded(probe.centreVerdict(job));
        job.cell().exposure(probe.exposure(job));
    }

    @Override
    public void resolveClick(WallCheckRequest job) {
        double margin = probe.pickSlack(job);
        job.cell().wallMargin(margin);
        job.cell().rayOccluded(MarginVerdict.of(margin));
        job.cell().centerOccluded(probe.centreVerdict(job));
        job.cell().exposure(probe.exposure(job));
    }
}
