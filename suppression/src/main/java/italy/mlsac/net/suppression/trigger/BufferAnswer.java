// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.trigger;

public record BufferAnswer(TriggerContext context, AnswerKind kind, SuspectOrigin origin) {

    public boolean evaluatesRules() {
        return kind == AnswerKind.SCORING && origin == SuspectOrigin.PLAYER;
    }
}
