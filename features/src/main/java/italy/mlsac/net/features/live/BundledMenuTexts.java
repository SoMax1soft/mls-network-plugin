// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.messages.BundledTexts;
import italy.mlsac.net.resource.ResourceSource;
import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.yaml.YamlFormatException;
import italy.mlsac.net.yaml.YamlParser;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class BundledMenuTexts {

    private BundledMenuTexts() {
    }

    public static BundledTexts load(ResourceSource resources, YamlParser yaml, PluginLog log) {
        Map<LanguageId, Map<String, Object>> files = new EnumMap<>(LanguageId.class);
        for (LanguageId language : LanguageId.values()) {
            String name = "menu_" + language.id() + ".yml";
            Optional<String> text = resources.text(name);
            if (text.isEmpty()) {
                continue;
            }
            try {
                files.put(language, yaml.parse(text.get()));
            } catch (YamlFormatException invalid) {
                log.warn(LogTopic.GENERAL, "Bundled " + name + " is unreadable: " + invalid.getMessage());
            }
        }
        return new BundledTexts(files);
    }
}
