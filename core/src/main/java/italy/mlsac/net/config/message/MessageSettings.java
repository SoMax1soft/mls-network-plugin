// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.message;

import italy.mlsac.net.text.LanguageId;
import java.util.Optional;

public record MessageSettings(MessageOverrides overrides, Optional<LanguageId> presetLanguage) {

    public static MessageSettings pluginDefaults() {
        return new MessageSettings(MessageOverrides.none(), Optional.empty());
    }
}
