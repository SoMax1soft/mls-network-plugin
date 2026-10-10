// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

import italy.mlsac.net.suppression.chain.ChainStage;
import italy.mlsac.net.suppression.trigger.TriggerContext;

public record FlagStageLine(TriggerContext context, ChainStage stage) implements StaffLineFormat {

    @Override
    public NoticeKey key() {
        return NoticeKey.ALERT_FLAG;
    }

    @Override
    public Placeholders placeholders(String actions) {
        return new Placeholders()
                .text("PLAYER", context.suspectName())
                .integer("VL", context.vl())
                .integer("STAGE", stage.position())
                .integer("STAGES", stage.total())
                .oneDecimal("BUFFER", context.buffer())
                .text("ACTIONS", actions);
    }
}
