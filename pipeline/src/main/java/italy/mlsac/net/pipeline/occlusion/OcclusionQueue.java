// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

import java.util.ArrayDeque;

public final class OcclusionQueue {
    private static final int CAPACITY = 400;

    private final ArrayDeque<WallCheckRequest> jobs = new ArrayDeque<>();
    private final OcclusionPolicy policy;
    private boolean pending;

    public OcclusionQueue(OcclusionPolicy policy) {
        this.policy = policy;
    }

    public void enqueue(WallCheckRequest job) {
        pending = true;
        jobs.addLast(job);
        if (jobs.size() > CAPACITY) {
            jobs.pollFirst();
        }
    }

    public void runAll(long rowTick) {
        if (pending) {
            policy.rowTick(rowTick);
            runAll();
        }
    }

    public void runAll() {
        if (!pending) {
            return;
        }
        while (!jobs.isEmpty()) {
            WallCheckRequest job = jobs.pollFirst();
            job.kind().resolve(policy, job);
        }
        pending = false;
    }

    public void abandon() {
        jobs.clear();
        pending = false;
    }
}
