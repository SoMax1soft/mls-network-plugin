// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import java.util.List;

public record StagePlan(List<InGameAction> inGame, List<CommandAction> commands) {

    public StagePlan {
        inGame = List.copyOf(inGame);
        commands = List.copyOf(commands);
    }

    public long lastsMillis() {
        DurationProbe probe = new DurationProbe();
        inGame.forEach(action -> action.offerTo(probe));
        return probe.longest;
    }

    private static final class DurationProbe implements InGameActionSink {

        private long longest;

        @Override
        public void timed(TimedAction action) {
            longest = Math.max(longest, action.lastsMillis());
        }

        @Override
        public void trick(TrickAction action) {
        }
    }
}
