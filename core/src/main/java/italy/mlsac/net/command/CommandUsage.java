// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.command;

import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class CommandUsage {

    private final MessageBook messages;

    public CommandUsage(MessageBook messages) {
        this.messages = messages;
    }

    public List<String> linesFor(CommandActor actor, Set<SubcommandName> available) {
        List<String> allowed = Stream.of(HelpEntry.values())
                .filter(entry -> available.contains(entry.verb) && entry.verb.permits(actor))
                .map(entry -> messages.text(entry.messageKey, TemplateValues.none()))
                .toList();
        String opening = messages.prefixed(allowed.isEmpty() ? "no-permission" : "usage-header", TemplateValues.none());
        return Stream.concat(Stream.of(opening), allowed.stream()).toList();
    }

    private enum HelpEntry {
        START(SubcommandName.START, "usage-start"),
        STOP(SubcommandName.STOP, "usage-stop"),
        DATASTATUS(SubcommandName.DATASTATUS, "usage-datastatus"),
        ALERTS(SubcommandName.ALERTS, "usage-alerts"),
        MONITOR(SubcommandName.MONITOR, "help-monitor"),
        SUSPECTS(SubcommandName.SUSPECTS, "usage-suspects"),
        PUNISH(SubcommandName.PUNISH, "usage-punish"),
        PROFILE(SubcommandName.PROFILE, "usage-profile"),
        RELOAD(SubcommandName.RELOAD, "usage-reload"),
        STATUS(SubcommandName.STATUS, "usage-status"),
        ANIMATION_LIST(SubcommandName.ANIMATION, "help-animation-list"),
        ANIMATION_RELOAD(SubcommandName.ANIMATION, "help-animation-reload"),
        REINSTALL(SubcommandName.REINSTALL, "help-reinstall"),
        DATA(SubcommandName.DATA, "usage-data");

        private final SubcommandName verb;
        private final String messageKey;

        HelpEntry(SubcommandName verb, String messageKey) {
            this.verb = verb;
            this.messageKey = messageKey;
        }
    }
}
