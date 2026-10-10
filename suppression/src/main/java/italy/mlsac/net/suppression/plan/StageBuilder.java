// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.plan;

import java.util.ArrayList;
import java.util.List;

public final class StageBuilder {

    private final List<InGameAction> inGame = new ArrayList<>();
    private final List<CommandAction> commands = new ArrayList<>();

    void inGame(InGameAction action) {
        inGame.add(action);
    }

    void command(CommandAction action) {
        commands.add(action);
    }

    StagePlan build() {
        return new StagePlan(inGame, commands);
    }
}
