// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.Vec3;

public final class MovingInterpolation implements Interpolation {
    private static final int STEPS = 3;
    private static final double HORIZONTAL_FLOOR = 0.05;
    private static final double VERTICAL_FLOOR = 0.08;

    private final SteppedBoxes boxes;
    private final Vec3 targetPoint;
    private final Vec3 offsets;
    private final boolean teleport;

    public MovingInterpolation(Box start, Vec3 targetPoint, Vec3 motion, Vec3 teleportJump, ClientQuirks profile) {
        this.targetPoint = targetPoint;
        this.boxes = new SteppedBoxes(start, Box.at(targetPoint), STEPS, 0, profile.skipsTicks() ? STEPS : 0);
        double offsetX = 3 * Math.max(motion.x(), HORIZONTAL_FLOOR) * 1.1 + 0.001;
        double offsetY = 3 * Math.max(motion.y(), VERTICAL_FLOOR) * 1.1 + 0.001;
        double offsetZ = 3 * Math.max(motion.z(), HORIZONTAL_FLOOR) * 1.1 + 0.001;
        if (teleportJump != null) {
            offsetX = Math.max(offsetX, teleportJump.x() + 0.001);
            offsetY = Math.max(offsetY, teleportJump.y() + 0.001);
            offsetZ = Math.max(offsetZ, teleportJump.z() + 0.001);
        }
        this.offsets = new Vec3(offsetX, offsetY, offsetZ);
        this.teleport = teleportJump != null;
        clamp();
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
        clamp();
    }

    private void clamp() {
        Box start = boxes.start();
        double[] x = clampAxis(start.low().x(), start.high().x(), targetPoint.x(), offsets.x());
        double[] y = clampAxis(start.low().y(), start.high().y(), targetPoint.y(), offsets.y());
        double[] z = clampAxis(start.low().z(), start.high().z(), targetPoint.z(), offsets.z());
        boxes.start(new Box(x[0], y[0], z[0], x[1], y[1], z[1]));
    }

    private double[] clampAxis(double min, double max, double centre, double offset) {
        double low = Math.max(min, centre - offset);
        double high = Math.min(max, centre + offset);
        if (low > high) {
            if (teleport) {
                return new double[] {min, max};
            }
            return new double[] {centre, centre};
        }
        return new double[] {low, high};
    }
}
