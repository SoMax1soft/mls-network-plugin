// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule;

public interface TaskScheduler {

    TaskTicket submit(TaskLane lane, TaskTiming timing, Runnable task);

    boolean onGlobalThread();

    void revokeEvery();
}
