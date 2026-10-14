// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.scoring.VerdictOrigin;

public enum StepAlertTemplate {
    MODEL_ANSWER("alert-format") {
        @Override
        void deliver(SubscriberBroadcast broadcast, SubscriberSet subscribers, String line) {
            broadcast.alert(subscribers, line);
        }
    },
    PACKET_INTEGRITY("alert-format-bad-packets") {
        @Override
        void deliver(SubscriberBroadcast broadcast, SubscriberSet subscribers, String line) {
            broadcast.consoleOnly(line, ConsoleCopy.DEBUG_ONLY);
        }
    };

    private final String messageKey;

    StepAlertTemplate(String messageKey) {
        this.messageKey = messageKey;
    }

    public static StepAlertTemplate of(VerdictOrigin origin) {
        return switch (origin) {
            case MODEL_ANSWER -> MODEL_ANSWER;
            case PACKET_INTEGRITY -> PACKET_INTEGRITY;
        };
    }

    public String messageKey() {
        return messageKey;
    }

    abstract void deliver(SubscriberBroadcast broadcast, SubscriberSet subscribers, String line);
}
