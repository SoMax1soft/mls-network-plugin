// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.command;

import java.util.Optional;
import java.util.stream.Stream;

enum VerbGate {
    CONSOLE_FOR_PLAYER_VERB("players-only") {
        @Override
        boolean closed(SubcommandName verb, CommandActor actor) {
            return verb.playerOnly() && !actor.isPlayer();
        }
    },
    MISSING_NODE("no-permission") {
        @Override
        boolean closed(SubcommandName verb, CommandActor actor) {
            return !verb.permits(actor);
        }
    };

    private final String refusalKey;

    VerbGate(String refusalKey) {
        this.refusalKey = refusalKey;
    }

    abstract boolean closed(SubcommandName verb, CommandActor actor);

    static Optional<String> refusal(SubcommandName verb, CommandActor actor) {
        return Stream.of(values()).filter(gate -> gate.closed(verb, actor)).map(gate -> gate.refusalKey).findFirst();
    }
}
