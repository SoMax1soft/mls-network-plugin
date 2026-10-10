// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.util.UUID;

public interface PlayerView {

    UUID id();

    String name();

    boolean online();

    boolean hasPermission(String node);

    boolean op();

    void sendMessage(String coloredText);

    void sendActionBar(String coloredText);

    void kick(String coloredReason);
}
