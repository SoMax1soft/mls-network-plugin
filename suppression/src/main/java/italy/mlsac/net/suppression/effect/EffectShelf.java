// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.effect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class EffectShelf<T extends TimedEffect> {

    static final int CAPACITY = 16;

    private final List<T> instances = new ArrayList<>();

    void add(T instance) {
        if (instances.size() >= CAPACITY) {
            instances.stream().min(Comparator.comparingLong(TimedEffect::end)).ifPresent(instances::remove);
        }
        instances.add(instance);
    }

    void pruneEndedBy(long playerTime) {
        instances.removeIf(instance -> instance.end() <= playerTime);
    }

    boolean isEmpty() {
        return instances.isEmpty();
    }

    List<T> copy() {
        return List.copyOf(instances);
    }
}
