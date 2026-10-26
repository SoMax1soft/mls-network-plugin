// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ActorPose {
    STANDING,
    EMERGING,
    ROARING,
    SNIFFING,
    DIGGING;

    static Optional<ActorPose> named(String name) {
        String wanted = name.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(pose -> pose.name().equals(wanted)).findFirst();
    }
}
