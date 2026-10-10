// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.alert;

public record AlertSettings(double threshold, boolean console, AlertSound sound) {

    public static AlertSettings pluginDefaults() {
        return new AlertSettings(0.80, false, AlertSound.pluginDefaults());
    }
}
