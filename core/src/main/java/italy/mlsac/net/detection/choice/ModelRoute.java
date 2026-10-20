// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice;

import italy.mlsac.net.api.ModelChoice;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum ModelRoute {
    SERVER_DEFAULT(ModelChoice.DEFAULT, Set.of()) {
        @Override
        public boolean runsUltra(boolean serverUltraMode) {
            return serverUltraMode;
        }

        @Override
        public boolean runsLegacy(boolean serverUltraMode) {
            return !serverUltraMode;
        }
    },
    PRO(ModelChoice.PRO, Set.of("pro")) {
        @Override
        public boolean runsUltra(boolean serverUltraMode) {
            return false;
        }

        @Override
        public boolean runsLegacy(boolean serverUltraMode) {
            return true;
        }
    },
    MINI(ModelChoice.MINI, Set.of("fast", "mini", "fast_mini")) {
        @Override
        public boolean runsUltra(boolean serverUltraMode) {
            return false;
        }

        @Override
        public boolean runsLegacy(boolean serverUltraMode) {
            return true;
        }
    },
    ULTRA(ModelChoice.ULTRA, Set.of("ultra")) {
        @Override
        public boolean runsUltra(boolean serverUltraMode) {
            return true;
        }

        @Override
        public boolean runsLegacy(boolean serverUltraMode) {
            return false;
        }
    };

    private final ModelChoice choice;
    private final Set<String> answerModels;

    ModelRoute(ModelChoice choice, Set<String> answerModels) {
        this.choice = choice;
        this.answerModels = answerModels;
    }

    public abstract boolean runsUltra(boolean serverUltraMode);

    public abstract boolean runsLegacy(boolean serverUltraMode);

    public ModelChoice choice() {
        return choice;
    }

    public boolean overrides() {
        return this != SERVER_DEFAULT;
    }

    public boolean answersWith(String verdictModel) {
        return answerModels.contains(normalised(verdictModel));
    }

    public static ModelRoute of(ModelChoice choice) {
        for (ModelRoute route : values()) {
            if (route.choice == choice) {
                return route;
            }
        }
        return SERVER_DEFAULT;
    }

    public static Optional<ModelChoice> answeringChoice(String verdictModel) {
        for (ModelRoute route : values()) {
            if (route.answersWith(verdictModel)) {
                return Optional.of(route.choice);
            }
        }
        return Optional.empty();
    }

    private static String normalised(String model) {
        return model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
    }
}
