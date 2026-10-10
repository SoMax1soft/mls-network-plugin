// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.messages.BundledTexts;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.text.LegacyColorizer;
import italy.mlsac.net.text.MessageBook;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public final class BundledResources {

    private BundledResources() {
    }

    public static Path root() {
        return Path.of(System.getProperty("mlsac.resources", "../core/src/main/resources"));
    }

    public static Map<String, Object> yaml(String relative) {
        try {
            String text = Files.readString(root().resolve(relative), StandardCharsets.UTF_8);
            Map<String, Object> parsed = new Yaml().load(text);
            return parsed == null ? Map.of() : parsed;
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static BundledTexts menus() {
        Map<LanguageId, Map<String, Object>> files = new EnumMap<>(LanguageId.class);
        for (LanguageId language : LanguageId.values()) {
            files.put(language, yaml("menu_" + language.id() + ".yml"));
        }
        return new BundledTexts(files);
    }

    public static HexColorNormalizer hex() {
        return new HexColorNormalizer();
    }

    public static LegacyColorizer colors() {
        return new LegacyColorizer(hex());
    }

    public static MessageBook english() {
        return TestConfigs.messages(new ConfigReference(EffectiveConfig.pluginDefaults(TestConfigs.core("language: en"))));
    }
}
