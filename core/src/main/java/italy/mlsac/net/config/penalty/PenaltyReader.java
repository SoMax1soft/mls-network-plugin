// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.penalty;

import italy.mlsac.net.config.CleanText;
import italy.mlsac.net.config.SettingsLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class PenaltyReader {

    private static final Pattern LEVEL = Pattern.compile("^[1-9][0-9]{0,2}$");
    private static final Pattern ANIMATION = Pattern.compile("^[A-Za-z0-9_-]{1,48}$");
    private static final int MAX_RUNGS = 50;
    private static final int MAX_COMMANDS = 50;
    private static final int MAX_COMMAND_LENGTH = 256;
    private static final String ANIMATION_PREFIX = "{ANIMATION}";
    private static final List<String> LEGACY_PREFIXES = List.of("{BAN}", "{KICK}");

    public PenaltySettings read(PenaltySettings lower, SettingsLayer layer) {
        boolean banShowOn = layer.bool("penalties.animation.enabled").orElse(lower.banShowOn());
        String banShowStyle = layer.matching("penalties.animation.type", ANIMATION).orElse(lower.banShowStyle());
        PenaltyLadder ladder = readLadder(layer).orElse(lower.ladder());
        boolean nickGuard = layer.bool("punishments.safe-name-check").orElse(lower.nickGuard());
        return new PenaltySettings(banShowOn, banShowStyle, ladder, nickGuard);
    }

    private Optional<PenaltyLadder> readLadder(SettingsLayer layer) {
        Optional<SettingsLayer> section = layer.section("penalties.actions");
        if (section.isEmpty()) {
            return Optional.empty();
        }
        TreeMap<Integer, List<String>> rungs = new TreeMap<>();
        for (Map.Entry<String, Object> entry : section.get().tree().asMap().entrySet()) {
            if (rungs.size() >= MAX_RUNGS) {
                break;
            }
            String key = entry.getKey().trim();
            if (!LEVEL.matcher(key).matches()) {
                section.get().drop(key, "invalid violation level");
                continue;
            }
            List<String> commands = commandsOf(entry.getValue());
            if (commands.isEmpty()) {
                section.get().drop(key, "no command");
                continue;
            }
            section.get().accepted();
            rungs.put(Integer.parseInt(key), commands);
        }
        return Optional.of(new PenaltyLadder(rungs));
    }

    private static List<String> commandsOf(Object value) {
        List<String> commands = new ArrayList<>();
        if (value instanceof String single) {
            addCommand(commands, single);
        } else if (value instanceof List<?> list) {
            for (Object element : list) {
                if (commands.size() >= MAX_COMMANDS) {
                    break;
                }
                if (element instanceof String text) {
                    addCommand(commands, text);
                }
            }
        }
        return commands;
    }

    private static void addCommand(List<String> commands, String raw) {
        String cleaned = CleanText.of(raw, MAX_COMMAND_LENGTH);
        if (!cleaned.isEmpty()) {
            commands.add(rewriteLegacyPrefix(cleaned));
        }
    }

    static String rewriteLegacyPrefix(String command) {
        for (String legacy : LEGACY_PREFIXES) {
            if (command.startsWith(legacy)) {
                return ANIMATION_PREFIX + command.substring(legacy.length());
            }
        }
        return command;
    }
}
