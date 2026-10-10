// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.geometry;

public final class BoxSpan {

    private static final int AXES = 3;

    private final Box start;
    private final double[] corners;
    private boolean grown;

    public BoxSpan(Box start) {
        this.start = start;
        this.corners = new double[] {start.low().x(), start.low().y(), start.low().z(),
                start.high().x(), start.high().y(), start.high().z()};
    }

    public void includePlayer(double x, double y, double z, double width, double height) {
        double half = Math.max(Box.THINNEST_HALF, width / 2.0);
        stretch(0, x - half, x + half);
        stretch(1, y, y + Math.max(Box.SHORTEST, height));
        stretch(2, z - half, z + half);
        grown = true;
    }

    public Box toBox() {
        if (!grown) {
            return start;
        }
        return new Box(corners[0], corners[1], corners[2], corners[AXES], corners[AXES + 1], corners[AXES + 2]);
    }

    private void stretch(int axis, double from, double to) {
        corners[axis] = Math.min(corners[axis], from);
        corners[axis + AXES] = Math.max(corners[axis + AXES], to);
    }
}
