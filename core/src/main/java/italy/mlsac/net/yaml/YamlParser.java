// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.yaml;

import java.util.Map;

public interface YamlParser {

    Map<String, Object> parse(String text) throws YamlFormatException;

    String dumpBlock(Map<String, Object> section);
}
