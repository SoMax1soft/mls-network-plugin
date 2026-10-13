// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.suppression.server.SuppressionTexts;
import italy.mlsac.net.text.MessageBook;
import java.util.Optional;

public final class MessageTexts implements SuppressionTexts {

    private final ConfigReference config;
    private final MessageBook messages;

    public MessageTexts(ConfigReference config, MessageBook messages) {
        this.config = config;
        this.messages = messages;
    }

    @Override
    public Optional<String> presetOverride(String key) {
        return config.get().messages().overrides().text(key);
    }

    @Override
    public String bundled(String key) {
        return messages.rawText(key);
    }

    @Override
    public String prefix() {
        return messages.rawPrefix();
    }
}
