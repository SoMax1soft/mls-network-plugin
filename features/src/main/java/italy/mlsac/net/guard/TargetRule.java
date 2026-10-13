// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Optional;
import java.util.regex.Pattern;

public final class TargetRule {

    private static final Pattern SHORT_FLAG = Pattern.compile("^-[A-Za-z]$");

    private final PlayerNames names;

    public TargetRule(PlayerNames names) {
        this.names = names;
    }

    public Optional<GuardRefusal> check(CommandLine line, int argument, Optional<PunishedPlayer> target) {
        int index = argument;
        while (line.argument(index).filter(word -> SHORT_FLAG.matcher(word).matches()).isPresent()) {
            index++;
        }
        Optional<String> word = line.argument(index);
        if (word.isEmpty()) {
            return Optional.of(new GuardRefusal(GuardReason.WRONG_TARGET, "missing"));
        }
        if (target.isEmpty()) {
            return names.plain(word.get()) || names.uuid(word.get())
                    ? Optional.empty()
                    : Optional.of(new GuardRefusal(GuardReason.WRONG_TARGET, word.get()));
        }
        PunishedPlayer player = target.get();
        if (word.get().equalsIgnoreCase(player.id().toString())) {
            return Optional.empty();
        }
        if (!names.plain(player.name())) {
            return Optional.of(new GuardRefusal(GuardReason.UNSAFE_NAME, player.name()));
        }
        return word.get().equals(player.name())
                ? Optional.empty()
                : Optional.of(new GuardRefusal(GuardReason.WRONG_TARGET, word.get()));
    }
}
