// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

public final class DerivedColumns {

    public static final int COUNT = 30;
    static final int SOURCE_COLUMNS = 50;
    private static final int BLENDS = 10;
    private static final int TRAILS = 10;
    private static final double LEAD_WEIGHT = 0.61803;
    private static final double LAG_WEIGHT = 0.38197;
    private static final double BLEND_SPREAD = 0.05;
    private static final double TRAIL_GAIN = 1.07;
    private static final double TRAIL_SPREAD = 0.03;
    private static final double LEAD_PHASE = 3.0;
    private static final double LAG_PHASE = 5.0;
    private static final double PHASE_RATE = 0.017;

    public double[] derive(double[][] rows, int tick, JitterStream jitter) {
        double[] row = rows[tick];
        double[] derived = new double[COUNT];
        for (int index = 0; index < BLENDS; index++) {
            derived[index] = blend(row, index, jitter.draw());
        }
        for (int index = BLENDS; index < BLENDS + TRAILS; index++) {
            int lag = 1 + index % 4;
            double[] earlier = tick - lag >= 0 ? rows[tick - lag] : row;
            derived[index] = trail(earlier, index, jitter.draw());
        }
        for (int index = BLENDS + TRAILS; index < COUNT; index++) {
            derived[index] = phase(row, index, tick);
        }
        return derived;
    }

    public static double phase(double[] row, int index, int tick) {
        double lead = present(row[(13 * index + 5) % SOURCE_COLUMNS]);
        double lag = present(row[(17 * index + 9) % SOURCE_COLUMNS]);
        double turn = (lead * LEAD_PHASE + lag * LAG_PHASE + tick) * PHASE_RATE;
        return ShortestFloat.asWritten((float) ((turn - Math.floor(turn)) * 2.0 - 1.0));
    }

    private static double blend(double[] row, int index, double jitter) {
        double lead = row[(7 * index + 3) % SOURCE_COLUMNS];
        double lag = row[(11 * index + 17) % SOURCE_COLUMNS];
        return ShortestFloat.asWritten((float) (lead * LEAD_WEIGHT - lag * LAG_WEIGHT
                + jitter * (BLEND_SPREAD * (Math.abs(lead) + 1.0))));
    }

    private static double trail(double[] earlier, int index, double jitter) {
        double source = earlier[(5 * index + 1) % SOURCE_COLUMNS];
        return ShortestFloat.asWritten((float) (source * TRAIL_GAIN
                + jitter * (TRAIL_SPREAD * (Math.abs(source) + 1.0))));
    }

    private static double present(double value) {
        return Double.isNaN(value) ? 0.0 : value;
    }
}
