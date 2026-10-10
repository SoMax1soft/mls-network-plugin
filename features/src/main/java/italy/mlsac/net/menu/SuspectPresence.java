// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.UUID;
import java.util.function.Predicate;

public interface SuspectPresence {

    boolean online(UUID player);

    String offlineNotice();

    static SuspectPresence of(Predicate<UUID> online, String offlineNotice) {
        return new SuspectPresence() {
            @Override
            public boolean online(UUID player) {
                return online.test(player);
            }

            @Override
            public String offlineNotice() {
                return offlineNotice;
            }
        };
    }
}
