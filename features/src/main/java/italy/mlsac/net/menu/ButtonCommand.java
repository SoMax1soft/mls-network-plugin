// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import java.util.regex.Pattern;

public sealed interface ButtonCommand permits ButtonCommand.Skip, ButtonCommand.Refused, ButtonCommand.Run {

    String PLAYER = "{PLAYER}";
    String REFUSAL_KEY = "menu-unsafe-name";
    Pattern PLAIN_NICK = Pattern.compile("^[A-Za-z0-9_]{1,16}$");

    static ButtonCommand prepare(String configured, String playerNick) {
        String command = configured == null ? "" : configured.trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isEmpty()) {
            return new Skip();
        }
        if (!command.contains(PLAYER)) {
            return new Run(command);
        }
        if (playerNick == null || !PLAIN_NICK.matcher(playerNick).matches()) {
            return new Refused();
        }
        return new Run(command.replace(PLAYER, playerNick));
    }

    void deliverTo(ButtonOutcomes outcomes);

    record Skip() implements ButtonCommand {
        @Override
        public void deliverTo(ButtonOutcomes outcomes) {
        }
    }

    record Refused() implements ButtonCommand {
        @Override
        public void deliverTo(ButtonOutcomes outcomes) {
            outcomes.refused();
        }
    }

    record Run(String command) implements ButtonCommand {
        @Override
        public void deliverTo(ButtonOutcomes outcomes) {
            outcomes.dispatch(command);
        }
    }
}
