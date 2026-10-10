// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.util.List;

public record GrantNode(String node) {
    private static final String NAMESPACE = "mlsac.";

    public static final GrantNode OPERATOR = beneath("admin");
    public static final GrantNode ALERT_FEED = beneath("notify.alerts");
    public static final GrantNode ALERT_READER = beneath("alerts");
    public static final GrantNode DETECTION_EXEMPT = beneath("bypass");
    public static final GrantNode SUPPRESSION_EXEMPT = beneath("suppression.bypass");

    public static GrantNode forCommand(String verb) {
        return beneath("command." + verb);
    }

    public static GrantNode older(String name) {
        return beneath(name);
    }

    public static List<GrantNode> standalone() {
        return List.of(OPERATOR, ALERT_FEED, ALERT_READER, DETECTION_EXEMPT, SUPPRESSION_EXEMPT);
    }

    private static GrantNode beneath(String suffix) {
        return new GrantNode(NAMESPACE + suffix);
    }
}
