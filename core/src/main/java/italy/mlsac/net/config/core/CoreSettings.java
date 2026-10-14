// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.core;

import italy.mlsac.net.config.guard.CommandGuardSettings;
import italy.mlsac.net.config.integrity.BadPacketSettings;
import italy.mlsac.net.text.LanguageId;
import java.util.Objects;

public record CoreSettings(boolean debug, LanguageId language, ApiKey accessKey, PresetSelection preset,
                           ServerIdentity identity, CommandGuardSettings commandGuard, BadPacketSettings badPackets) {

    public CoreSettings {
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(accessKey, "apiKey");
        Objects.requireNonNull(preset, "preset");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(commandGuard, "commandGuard");
        Objects.requireNonNull(badPackets, "badPackets");
    }
}
