// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.geometry.Box;

final class SteppedBoxes {
    private Box start;
    private final Box target;
    private final int steps;
    private int low;
    private int high;

    SteppedBoxes(Box start, Box target, int steps, int low, int high) {
        this.start = start;
        this.target = target;
        this.steps = steps;
        this.low = low;
        this.high = high;
    }

    Box start() {
        return start;
    }

    void start(Box replacement) {
        start = replacement;
    }

    Box target() {
        return target;
    }

    void advance() {
        high = steps;
    }

    void advanceWithLow() {
        low = Math.min(low + 1, steps);
        high = steps;
    }

    Box possible() {
        Box union = at(low);
        for (int step = low + 1; step <= high; step++) {
            union = union.union(at(step));
        }
        return union;
    }

    private Box at(int step) {
        return new Box(
                bound(start.low().x(), target.low().x(), step),
                bound(start.low().y(), target.low().y(), step),
                bound(start.low().z(), target.low().z(), step),
                bound(start.high().x(), target.high().x(), step),
                bound(start.high().y(), target.high().y(), step),
                bound(start.high().z(), target.high().z(), step));
    }

    private double bound(double from, double to, int step) {
        double slope = (to - from) / steps;
        return from + step * slope;
    }
}
