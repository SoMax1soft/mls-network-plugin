// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ActorAction {
    SPAWN,
    POSE,
    FACE_VICTIM,
    STEP,
    ATTACK,
    SHAKE_TENDRILS,
    SONIC_BOOM,
    TURN_VICTIM,
    SWALLOW_VICTIM,
    REMOVE;

    static Optional<ActorAction> named(String name) {
        String wanted = name.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(action -> action.name().equals(wanted)).findFirst();
    }
}
