// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.UUID;

public sealed interface MenuReaction permits MenuReaction.Nothing, MenuReaction.Redraw, MenuReaction.Close,
        MenuReaction.CloseAndRun, MenuReaction.OpenChecks, MenuReaction.OpenSuspects, MenuReaction.CloseAndTeleport,
        MenuReaction.Tell, MenuReaction.OpenKicks, MenuReaction.OpenHistory, MenuReaction.OpenProfile {

    void carryOut(MenuActions actions);

    record Nothing() implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
        }
    }

    record Redraw(MenuFrame frame) implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
            actions.redraw(frame);
        }
    }

    record Close() implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
            actions.close();
        }
    }

    record CloseAndRun(ButtonCommand command) implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
            actions.close();
            actions.runButton(command);
        }
    }

    record OpenChecks(UUID suspect) implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
            actions.openChecks(suspect);
        }
    }

    record OpenSuspects() implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
            actions.openSuspects();
        }
    }

    record CloseAndTeleport(UUID suspect) implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
            actions.close();
            actions.teleport(suspect);
        }
    }

    record Tell(String coloredMessage) implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
            actions.tell(coloredMessage);
        }
    }

    record OpenKicks(int page) implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
            actions.openKicks(page);
        }
    }

    record OpenHistory(String player, int page, boolean withSuppressions, MenuReaction back) implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
            actions.openHistory(this);
        }

        OpenHistory turnedTo(int target) {
            return new OpenHistory(player, target, withSuppressions, back);
        }
    }

    record OpenProfile(String player) implements MenuReaction {
        @Override
        public void carryOut(MenuActions actions) {
            actions.openProfile(player);
        }
    }
}
