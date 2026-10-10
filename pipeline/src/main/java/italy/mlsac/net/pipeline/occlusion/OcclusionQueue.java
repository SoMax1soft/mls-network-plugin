// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

import java.util.ArrayDeque;

public final class OcclusionQueue {
    private static final int CAPACITY = 400;

    private final ArrayDeque<WallCheckRequest> jobs = new ArrayDeque<>();
    private final OcclusionPolicy policy;

    public OcclusionQueue(OcclusionPolicy policy) {
        this.policy = policy;
    }

    public void enqueue(WallCheckRequest job) {
        jobs.addLast(job);
        if (jobs.size() > CAPACITY) {
            jobs.pollFirst();
        }
    }

    public void runAll() {
        while (!jobs.isEmpty()) {
            WallCheckRequest job = jobs.pollFirst();
            job.kind().resolve(policy, job);
        }
    }

    public void abandon() {
        jobs.clear();
    }
}
