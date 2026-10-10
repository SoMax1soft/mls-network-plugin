// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuCanvas {

    void open(UUID viewer, MenuFrame frame);

    void redraw(UUID viewer, MenuFrame frame);

    void close(UUID viewer);

    void send(UUID viewer, String coloredText);

    void teleport(UUID viewer, UUID target);

    void runConsoleNextTick(String command);

    void onViewerThread(UUID viewer, Runnable task);

    List<SuspectRow> onlineSuspects();

    Optional<SuspectRow> suspect(UUID player);

    boolean online(UUID player);
}
