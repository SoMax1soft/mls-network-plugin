// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.List;

public record TagTemplates(List<String> lines, String fastItem, String proItem, String ultraItem, String pulseItem) {

    public TagTemplates {
        lines = lines.isEmpty() ? stock().lines() : List.copyOf(lines);
    }

    static TagTemplates stock() {
        return new TagTemplates(List.of("&f&lAVG: {AVG}% &7| {HIST}"), "&2F&f{RESULT}%", "&6P&f{RESULT}%",
                "&cU&f{RESULT}%", "&bPu&f{RESULT}%");
    }
}
