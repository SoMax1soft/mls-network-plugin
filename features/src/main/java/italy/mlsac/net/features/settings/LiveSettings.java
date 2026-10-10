// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.settings;

import italy.mlsac.net.config.EffectiveConfig;
import java.util.function.Supplier;

public final class LiveSettings implements Supplier<FeatureSettings> {

    private final FeatureSettingsTranslation translation = new FeatureSettingsTranslation();
    private volatile FeatureSettings current;

    public LiveSettings(EffectiveConfig initial) {
        this.current = translation.translate(initial);
    }

    public void refresh(EffectiveConfig config) {
        current = translation.translate(config);
    }

    @Override
    public FeatureSettings get() {
        return current;
    }
}
