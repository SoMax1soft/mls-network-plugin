// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

public enum LayerSource {
    LOCAL_FILE(false),
    PRESET(true);

    private final boolean replacesWholesale;

    LayerSource(boolean replacesWholesale) {
        this.replacesWholesale = replacesWholesale;
    }

    public boolean replacesWholesale() {
        return replacesWholesale;
    }
}
