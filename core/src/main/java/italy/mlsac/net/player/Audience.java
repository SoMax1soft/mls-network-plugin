// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.util.function.Predicate;

public enum Audience implements Predicate<PlayerView> {
    STAFF {
        @Override
        public boolean test(PlayerView player) {
            return isStaff(player);
        }
    },
    STAFF_OR_OP {
        @Override
        public boolean test(PlayerView player) {
            return player.op() || isStaff(player);
        }
    },
    ADMIN_OR_OP {
        @Override
        public boolean test(PlayerView player) {
            return player.op() || player.hasPermission(GrantNode.OPERATOR.node());
        }
    };

    private static boolean isStaff(PlayerView player) {
        return player.hasPermission(GrantNode.ALERT_FEED.node())
                || player.hasPermission(GrantNode.ALERT_READER.node())
                || player.hasPermission(GrantNode.OPERATOR.node());
    }
}
