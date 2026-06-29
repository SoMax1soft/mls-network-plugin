package wtf.mlsac.server;

import org.junit.jupiter.api.Test;
import wtf.mlsac.scheduler.ScheduledTask;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedTaskTest {

    /** Minimal stub that just records whether it was cancelled. */
    private static final class StubTask implements ScheduledTask {
        private boolean cancelled;

        @Override
        public void cancel() {
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isRunning() {
            return false;
        }
    }

    @Test
    void rescheduleCancelsThePreviousTask() {
        ManagedTask task = new ManagedTask();
        StubTask first = new StubTask();
        StubTask second = new StubTask();

        task.reschedule(() -> first);
        assertTrue(task.isScheduled());

        task.reschedule(() -> second);
        assertTrue(first.isCancelled(), "previous task must be cancelled on reschedule");
        assertFalse(second.isCancelled());
        assertTrue(task.isScheduled());
    }

    @Test
    void cancelCancelsAndForgets() {
        ManagedTask task = new ManagedTask();
        StubTask stub = new StubTask();
        task.reschedule(() -> stub);

        task.cancel();
        assertTrue(stub.isCancelled());
        assertFalse(task.isScheduled());

        // Cancelling again is a harmless no-op.
        task.cancel();
    }

    @Test
    void clearReferenceForgetsWithoutCancelling() {
        ManagedTask task = new ManagedTask();
        StubTask stub = new StubTask();
        task.reschedule(() -> stub);

        task.clearReference();
        assertFalse(task.isScheduled());
        assertFalse(stub.isCancelled(), "clearReference must not cancel an already-finished task");
    }
}
