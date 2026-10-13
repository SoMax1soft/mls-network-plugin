// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

import italy.mlsac.net.suppression.plan.TrickTuning;
import italy.mlsac.net.suppression.plan.TrickType;
import italy.mlsac.net.suppression.server.SuppressionNotices;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.Optional;

public final class TrickBroadcast {

    private final TrickTextChoice choice;
    private final SuppressionNotices notices;

    public TrickBroadcast(TrickTextChoice choice, SuppressionNotices notices) {
        this.choice = choice;
        this.notices = notices;
    }

    public Optional<String> compose(TrickType type, TrickTuning tuning, TriggerContext context) {
        return choice.templateFor(type, tuning).map(template -> template.render(new Placeholders()
                .text("PLAYER", context.suspectName())
                .oneDecimal("BUFFER", context.buffer())
                .oneDecimal("DETECTIONS", context.buffer())
                .twoDecimals("PROBABILITY", context.probability())
                .integer("VL", context.vl())));
    }

    public void announce(TrickType type, TrickTuning tuning, TriggerContext context) {
        compose(type, tuning, context).ifPresent(this::say);
    }

    public TrickAnnouncements forOneEvent() {
        return new TrickAnnouncements(this);
    }

    void say(String line) {
        notices.toStaffAndOps(line);
    }
}
