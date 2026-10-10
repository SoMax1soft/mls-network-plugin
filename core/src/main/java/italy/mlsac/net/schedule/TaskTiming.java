// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule;

public record TaskTiming(Cadence cadence, long delayTicks, long everyTicks) {

    private static final TaskTiming IMMEDIATE = new TaskTiming(Cadence.NOW, 0L, 0L);

    public enum Cadence {
        NOW,
        ONCE_LATER,
        REPEATING
    }

    public static TaskTiming immediately() {
        return IMMEDIATE;
    }

    public static TaskTiming after(long delayTicks) {
        return new TaskTiming(Cadence.ONCE_LATER, delayTicks, 0L);
    }

    public static TaskTiming repeating(long delayTicks, long everyTicks) {
        return new TaskTiming(Cadence.REPEATING, delayTicks, everyTicks);
    }

    public boolean repeats() {
        return cadence == Cadence.REPEATING;
    }
}
