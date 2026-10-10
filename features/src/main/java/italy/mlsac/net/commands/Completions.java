// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.commands;

import java.util.List;
import java.util.Locale;

public final class Completions {

    private Completions() {
    }

    public static List<String> matching(List<String> candidates, String typed) {
        String prefix = typed == null ? "" : typed.toLowerCase(Locale.ROOT);
        return candidates.stream().filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }

    public static String last(List<String> args) {
        return args.isEmpty() ? "" : args.get(args.size() - 1);
    }
}
