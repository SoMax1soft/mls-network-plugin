// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import java.util.Objects;
import java.util.function.Supplier;

public final class ConfigReference implements Supplier<EffectiveConfig> {

    private volatile EffectiveConfig current;

    public ConfigReference(EffectiveConfig initial) {
        this.current = Objects.requireNonNull(initial, "initial");
    }

    @Override
    public EffectiveConfig get() {
        return current;
    }

    public void publish(EffectiveConfig config) {
        this.current = Objects.requireNonNull(config, "config");
    }
}
