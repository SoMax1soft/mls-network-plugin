// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

import italy.mlsac.net.suppression.trigger.BufferTrigger;

public record BufferTriggerLine(BufferTrigger trigger) implements StaffLineFormat {

    @Override
    public NoticeKey key() {
        return NoticeKey.ALERT_BUFFER;
    }

    @Override
    public Placeholders placeholders(String actions) {
        return new Placeholders()
                .text("PLAYER", trigger.context().suspectName())
                .integer("VL", trigger.context().vl())
                .integer("PERCENT", trigger.highestPercent())
                .oneDecimal("BUFFER", trigger.context().buffer())
                .text("ACTIONS", actions);
    }
}
