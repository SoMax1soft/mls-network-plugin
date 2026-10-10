// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import java.util.ArrayDeque;
import java.util.function.IntSupplier;

final class AcknowledgedWork {
    private static final int QUEUE_CAP = 8192;

    private final ArrayDeque<Runnable> jobs = new ArrayDeque<>();
    private final ArrayDeque<Integer> thresholds = new ArrayDeque<>();
    private final IntSupplier answered;

    AcknowledgedWork(IntSupplier answered) {
        this.answered = answered;
    }

    void enqueue(int threshold, Runnable job) {
        if (answered.getAsInt() >= threshold) {
            job.run();
            return;
        }
        if (jobs.size() >= QUEUE_CAP) {
            thresholds.pollFirst();
            jobs.pollFirst().run();
        }
        thresholds.addLast(threshold);
        jobs.addLast(job);
    }

    void releaseReached() {
        int reached = answered.getAsInt();
        while (!thresholds.isEmpty() && thresholds.peekFirst() <= reached) {
            thresholds.pollFirst();
            jobs.pollFirst().run();
        }
    }
}
