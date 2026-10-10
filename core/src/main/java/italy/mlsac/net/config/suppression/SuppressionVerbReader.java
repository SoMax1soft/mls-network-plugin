// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import italy.mlsac.net.config.SettingsLayer;
import java.util.Optional;

interface SuppressionVerbReader {

    String type();

    boolean allowedInBufferRules();

    Optional<SuppressionAction> parse(SettingsLayer action, int stage);
}
