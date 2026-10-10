// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.List;

public record TagLines(List<String> topFirst) {

    static final String LINE_BREAK = "\n&r";

    public TagLines {
        if (topFirst.isEmpty()) {
            throw new IllegalArgumentException("a tag needs at least one line");
        }
        topFirst = List.copyOf(topFirst);
    }

    public static TagLines single(String line) {
        return new TagLines(List.of(line));
    }

    public int count() {
        return topFirst.size();
    }

    public String fromBottom(int level) {
        return topFirst.get(topFirst.size() - 1 - level);
    }

    public String stacked() {
        return String.join(LINE_BREAK, topFirst);
    }
}
