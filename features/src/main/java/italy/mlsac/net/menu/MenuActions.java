// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.UUID;

public interface MenuActions {

    void redraw(MenuFrame frame);

    void close();

    void runButton(ButtonCommand command);

    void openChecks(UUID suspect);

    void openSuspects();

    void teleport(UUID suspect);

    void tell(String coloredMessage);

    void openKicks(int page);

    void openHistory(MenuReaction.OpenHistory request);

    void openProfile(String player);
}
