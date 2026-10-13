// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public record CommandLine(String text, String label, Optional<String> namespace, String bare, List<String> args) {

    public CommandLine {
        args = List.copyOf(args);
    }

    public static CommandLine parse(String command) {
        String text = command.trim().replaceAll(" {2,}", " ");
        if (text.startsWith("/")) {
            text = text.substring(1);
        }
        String[] words = text.split(" ");
        String label = words[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        Optional<String> namespace = colon > 0 ? Optional.of(label.substring(0, colon)) : Optional.empty();
        String bare = colon >= 0 ? label.substring(colon + 1) : label;
        return new CommandLine(text, label, namespace, bare, Arrays.asList(words).subList(1, words.length));
    }

    public Optional<String> argument(int index) {
        return index >= 0 && index < args.size() ? Optional.of(args.get(index)) : Optional.empty();
    }

    public Optional<String> firstArgumentLowered() {
        return argument(0).map(word -> word.toLowerCase(Locale.ROOT));
    }
}
