// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import java.util.ArrayDeque;
import java.util.List;

final class RollingWindow {

    private final int capacity;
    private final ArrayDeque<Double> values;

    RollingWindow(int capacity) {
        this.capacity = capacity;
        this.values = new ArrayDeque<>(capacity);
    }

    void add(double value) {
        if (values.size() == capacity) {
            values.removeFirst();
        }
        values.addLast(value);
    }

    List<Double> oldestFirst() {
        return List.copyOf(values);
    }
}
