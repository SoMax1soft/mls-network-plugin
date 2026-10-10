// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.suppression;

import italy.mlsac.net.config.SettingsLayer;
import italy.mlsac.net.config.response.TrickKind;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class ActionParsers {

    private final Map<String, SuppressionVerbReader> byType = new LinkedHashMap<>();

    ActionParsers() {
        register(new CommandActionParser());
        register(PercentActionParser.damageDealt());
        register(PercentActionParser.damageTaken());
        register(new LagActionParser());
        for (TrickKind kind : TrickKind.values()) {
            register(new TrickActionParser(kind));
        }
    }

    private void register(SuppressionVerbReader parser) {
        byType.put(parser.type(), parser);
    }

    Optional<SuppressionAction> parse(SettingsLayer action, boolean inBufferRule) {
        String type = action.text("type", 32).orElse("").toLowerCase(Locale.ROOT);
        SuppressionVerbReader parser = byType.get(type);
        if (parser == null) {
            action.drop("type", "unknown action type '" + type + "'");
            return Optional.empty();
        }
        if (inBufferRule && !parser.allowedInBufferRules()) {
            action.drop("type", "commands are not allowed in buffer rules");
            return Optional.empty();
        }
        int stage = 0;
        if (action.has("stage")) {
            Optional<Integer> parsed = action.strictInt("stage", 0, 7);
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            stage = parsed.get();
        }
        return parser.parse(action, stage);
    }
}
