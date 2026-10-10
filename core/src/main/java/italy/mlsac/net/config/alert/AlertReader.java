// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.alert;

import italy.mlsac.net.config.SettingsLayer;
import java.util.regex.Pattern;

public final class AlertReader {

    private static final Pattern SOUND = Pattern.compile("^[A-Za-z0-9_.]{1,64}$");
    private static final String SOUND_SECTION = "alerts.sound.";

    public AlertSettings read(AlertSettings lower, SettingsLayer layer) {
        double threshold = layer.clamped("alerts.threshold", 0.0, 1.0).orElse(lower.threshold());
        boolean console = layer.bool("alerts.console").orElse(lower.console());
        AlertSound base = lower.sound();
        AlertSound sound = new AlertSound(
                layer.bool(SOUND_SECTION + "enabled").orElse(base.enabled()),
                layer.matching(SOUND_SECTION + "type", SOUND).orElse(base.type()),
                (float) layer.clamped(SOUND_SECTION + "volume", 0.0, 10.0).orElse(base.volume()),
                (float) layer.clamped(SOUND_SECTION + "pitch", 0.5, 2.0).orElse(base.pitch()));
        return new AlertSettings(threshold, console, sound);
    }
}
