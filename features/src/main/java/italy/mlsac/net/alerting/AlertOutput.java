// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

public record AlertOutput(boolean console, AlertSound sound) {

    public static AlertOutput pluginDefaults() {
        return new AlertOutput(false, AlertSound.pluginDefaults());
    }
}
