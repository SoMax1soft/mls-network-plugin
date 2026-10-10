// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule;

public final class InertTask implements TaskTicket {

    private volatile boolean cancelled;

    @Override
    public void cancel() {
        cancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }
}
