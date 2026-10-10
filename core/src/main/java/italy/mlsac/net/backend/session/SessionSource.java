// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.config.core.CoreSettings;

@FunctionalInterface
public interface SessionSource {

    RunningSession open(CoreSettings core);
}
