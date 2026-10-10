// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server;

import java.util.UUID;

public interface ServerThreads {

    void onSuspectThread(UUID suspectId, Runnable whileOnline, Runnable whenOffline);

    void onGlobalThread(Runnable task);
}
