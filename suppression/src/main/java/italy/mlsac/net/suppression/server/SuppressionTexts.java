// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server;

import java.util.Optional;

public interface SuppressionTexts {

    Optional<String> presetOverride(String key);

    String bundled(String key);

    String prefix();
}
