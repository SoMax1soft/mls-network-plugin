// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.geometry.Box;

public final class FrozenInterpolation implements Interpolation {
    private final SteppedBoxes boxes;

    public FrozenInterpolation(Box held) {
        this.boxes = new SteppedBoxes(held, held, 1, 0, 0);
    }

    @Override
    public void advance() {
        boxes.advance();
    }

    @Override
    public void advanceWithLow() {
        boxes.advanceWithLow();
    }

    @Override
    public Box possible() {
        return boxes.possible();
    }

    @Override
    public void widenStart(Box extra) {
        boxes.start(boxes.start().union(extra));
    }
}
