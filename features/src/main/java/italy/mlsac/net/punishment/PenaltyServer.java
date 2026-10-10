// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.UUID;

public interface PenaltyServer {

    void dispatchConsoleNextTick(String command);

    boolean online(UUID player);

    void kickNextTick(UUID player, String reason);

    void punishReport(PunishEvent event);

    default void handedOver(String configuredCommand) {
    }
}
