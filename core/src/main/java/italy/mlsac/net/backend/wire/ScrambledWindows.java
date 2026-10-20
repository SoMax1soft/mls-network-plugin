// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

public final class ScrambledWindows {

    public static final int SLOTS = PlainWindow.WIDTH + DerivedColumns.COUNT;
    public static final int WIDE_SLOTS = PlainWindow.WIDE_WIDTH + DerivedColumns.COUNT;

    private final int plainWidth;
    private final SlotOrder order;
    private final JitterStream jitter;
    private final DerivedColumns derived = new DerivedColumns();

    public ScrambledWindows(WireSeed seed) {
        this(seed, PlainWindow.WIDTH);
    }

    public ScrambledWindows(WireSeed seed, int plainWidth) {
        this.plainWidth = plainWidth;
        this.order = SlotOrder.shuffled(seed, plainWidth + DerivedColumns.COUNT);
        this.jitter = new JitterStream(seed);
    }

    public int slots() {
        return order.width();
    }

    public synchronized double[][] scramble(PlainWindow window) {
        if (window.width() != plainWidth) {
            throw new IllegalArgumentException("window rows hold " + window.width() + " values, this session sends "
                    + plainWidth);
        }
        double[][] rows = window.rows();
        double[][] sent = new double[rows.length][];
        for (int tick = 0; tick < rows.length; tick++) {
            double[] extended = new double[order.width()];
            System.arraycopy(rows[tick], 0, extended, 0, plainWidth);
            System.arraycopy(derived.derive(rows, tick, jitter), 0, extended, plainWidth, DerivedColumns.COUNT);
            sent[tick] = order.arrange(extended);
        }
        return sent;
    }
}
