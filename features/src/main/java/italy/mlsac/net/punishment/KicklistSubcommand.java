// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import italy.mlsac.net.command.CommandActor;
import italy.mlsac.net.command.Subcommand;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.DisplayNumbers;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;

public final class KicklistSubcommand implements Subcommand {

    static final int ENTRIES_PER_SCREEN = 10;
    static final String SEPARATOR = "&8" + "- ".repeat(16).trim();

    private final KickHistory history;
    private final MessageBook messages;

    public KicklistSubcommand(KickHistory history, MessageBook messages) {
        this.history = history;
        this.messages = messages;
    }

    @Override
    public SubcommandName name() {
        return SubcommandName.KICKLIST;
    }

    @Override
    public void execute(CommandActor invoker, List<String> args) {
        List<KickLogLine> records = history.newestFirst();
        if (records.isEmpty()) {
            invoker.send(messages.prefixed("kicklist-empty"));
            return;
        }
        int maxPage = (records.size() + ENTRIES_PER_SCREEN - 1) / ENTRIES_PER_SCREEN;
        int page = Math.max(1, Math.min(maxPage, requestedPage(args)));
        invoker.send(messages.prefixed("kicklist-header", TemplateValues.of("{PAGE}", page).and("{MAX_PAGE}", maxPage)));
        invoker.send(messages.colorize(SEPARATOR));
        int first = (page - 1) * ENTRIES_PER_SCREEN;
        for (int i = first; i < Math.min(records.size(), first + ENTRIES_PER_SCREEN); i++) {
            invoker.send(messages.colorize(line(i + 1, records.get(i))));
        }
        invoker.send(messages.colorize(SEPARATOR));
        if (page < maxPage) {
            invoker.send(messages.prefixed("kicklist-footer", TemplateValues.of("{NEXT_PAGE}", page + 1)));
        }
    }

    private static int requestedPage(List<String> args) {
        if (args.isEmpty()) {
            return 1;
        }
        try {
            return Integer.parseInt(args.get(0));
        } catch (NumberFormatException invalid) {
            return 1;
        }
    }

    private static String line(int index, KickLogLine record) {
        return "&e" + index + ". &f" + record.playerNick() + " &7[&c" + record.localTime() + "&7] &8- &bProb: &f"
                + DisplayNumbers.twoDecimals(record.probability()) + " &8| &bBuf: &f" + DisplayNumbers.oneDecimal(record.buffer())
                + " &8| &bVL: &f" + record.offenceLevel();
    }
}
