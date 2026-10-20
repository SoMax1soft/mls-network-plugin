// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice;

import italy.mlsac.net.api.FallbackReason;
import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.config.detection.DetectionSettings;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ModelAvailability(DetectionSettings detection, Map<ModelChoice, FallbackReason> refusals) {

    private static final String PRO_KEY = "pro";
    private static final String MINI_KEY = "fast";

    public ModelAvailability {
        Objects.requireNonNull(detection, "detection");
        EnumMap<ModelChoice, FallbackReason> copy = new EnumMap<>(ModelChoice.class);
        copy.putAll(refusals);
        refusals = Collections.unmodifiableMap(copy);
    }

    public static ModelAvailability of(DetectionSettings detection) {
        return new ModelAvailability(detection, Map.of());
    }

    public Optional<FallbackReason> blocker(ModelChoice choice) {
        if (choice == ModelChoice.DEFAULT) {
            return Optional.empty();
        }
        if (!detection.enabled()) {
            return Optional.of(FallbackReason.DETECTION_OFF);
        }
        if (switchedOffInPreset(choice)) {
            return Optional.of(FallbackReason.DISABLED_IN_PRESET);
        }
        return Optional.ofNullable(refusals.get(choice));
    }

    public Set<ModelChoice> available() {
        EnumSet<ModelChoice> open = EnumSet.noneOf(ModelChoice.class);
        for (ModelChoice choice : ModelChoice.values()) {
            if (blocker(choice).isEmpty()) {
                open.add(choice);
            }
        }
        return Collections.unmodifiableSet(open);
    }

    public boolean refuses(ModelChoice choice) {
        return refusals.containsKey(choice);
    }

    public ModelAvailability withRefusal(ModelChoice choice, FallbackReason reason) {
        EnumMap<ModelChoice, FallbackReason> more = new EnumMap<>(ModelChoice.class);
        more.putAll(refusals);
        more.put(choice, reason);
        return new ModelAvailability(detection, more);
    }

    private boolean switchedOffInPreset(ModelChoice choice) {
        return choice == ModelChoice.PRO && !detection.models().enabled(PRO_KEY)
                || choice == ModelChoice.MINI && !detection.models().enabled(MINI_KEY);
    }
}
