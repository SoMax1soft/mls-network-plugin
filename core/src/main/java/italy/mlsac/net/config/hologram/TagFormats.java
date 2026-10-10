// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.hologram;

import java.util.List;

public record TagFormats(List<String> lines, String fastItem, String proItem, String ultraItem, String pulseItem) {

    public TagFormats {
        lines = List.copyOf(lines);
    }

    static TagFormats stock() {
        return new TagFormats(List.of("&f&lAVG: {AVG}% &7| {HIST}"), "&2F&f{RESULT}%", "&6P&f{RESULT}%",
                "&cU&f{RESULT}%", "&bPu&f{RESULT}%");
    }
}
