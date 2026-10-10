// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.testkit;

import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.ConfigTree;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.config.core.CoreSettingsReader;
import italy.mlsac.net.resource.ClasspathResources;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.text.LegacyColorizer;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.MessageCatalog;
import italy.mlsac.net.yaml.SnakeYamlParser;
import italy.mlsac.net.yaml.YamlFormatException;
import java.util.Map;

public final class TestConfigs {

    private TestConfigs() {
    }

    public static ConfigTree yaml(String text) {
        try {
            return ConfigTree.of(new SnakeYamlParser().parse(text));
        } catch (YamlFormatException invalid) {
            throw new IllegalArgumentException(invalid);
        }
    }

    public static CoreSettings core(String configYaml) {
        return new CoreSettingsReader().read(yaml(configYaml)).settings();
    }

    public static CoreSettings core() {
        return new CoreSettingsReader().read(ConfigTree.of(Map.of())).settings();
    }

    public static EffectiveConfig defaults() {
        return EffectiveConfig.pluginDefaults(core());
    }

    public static MessageBook messages(ConfigReference config) {
        try {
            MessageCatalog catalog = MessageCatalog.load(new ClasspathResources(TestConfigs.class.getClassLoader()),
                    new SnakeYamlParser());
            return new MessageBook(catalog, config, new LegacyColorizer(new HexColorNormalizer()));
        } catch (YamlFormatException invalid) {
            throw new IllegalStateException(invalid);
        }
    }
}
