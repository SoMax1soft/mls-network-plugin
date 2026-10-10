// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server;

import java.util.UUID;

public interface SuspectStatus {

    boolean online(UUID suspectId);

    boolean effectsPaused(UUID suspectId);
}
