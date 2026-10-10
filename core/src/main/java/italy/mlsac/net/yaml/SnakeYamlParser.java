// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.yaml;

import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public final class SnakeYamlParser implements YamlParser {

    @Override
    public Map<String, Object> parse(String text) throws YamlFormatException {
        try {
            Object loaded = new Yaml().load(text);
            Map<String, Object> result = new LinkedHashMap<>();
            if (loaded instanceof Map<?, ?> map) {
                map.forEach((key, value) -> result.put(String.valueOf(key), value));
            }
            return result;
        } catch (RuntimeException invalid) {
            throw new YamlFormatException("invalid YAML: " + invalid.getMessage(), invalid);
        }
    }

    @Override
    public String dumpBlock(Map<String, Object> section) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        return new Yaml(options).dump(section);
    }
}
