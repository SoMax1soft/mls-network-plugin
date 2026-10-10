// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.occlusion;

public final class OcclusionCell {
    private int rayOccluded = -1;
    private int centerOccluded = -1;
    private double exposure = -1.0;
    private double wallMargin = -1.0;

    public static OcclusionCell of(int rayOccluded, int centerOccluded, double exposure, double wallMargin) {
        OcclusionCell cell = new OcclusionCell();
        cell.rayOccluded = rayOccluded;
        cell.centerOccluded = centerOccluded;
        cell.exposure = exposure;
        cell.wallMargin = wallMargin;
        return cell;
    }

    public int rayOccluded() {
        return rayOccluded;
    }

    public int centerOccluded() {
        return centerOccluded;
    }

    public double exposure() {
        return exposure;
    }

    public double wallMargin() {
        return wallMargin;
    }

    void rayOccluded(int value) {
        rayOccluded = value;
    }

    void centerOccluded(int value) {
        centerOccluded = value;
    }

    void exposure(double value) {
        exposure = value;
    }

    void wallMargin(double value) {
        wallMargin = value;
    }
}
