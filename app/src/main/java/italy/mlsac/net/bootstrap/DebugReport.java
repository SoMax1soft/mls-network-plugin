// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import java.util.List;

interface DebugReport {

    void refreshNow();

    List<String> summary();
}
