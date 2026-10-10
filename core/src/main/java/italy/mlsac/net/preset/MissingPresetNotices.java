// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.preset;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.player.Audience;
import italy.mlsac.net.player.NoticeBoard;
import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MissingPresetNotices {

    static final long JOIN_DELAY_TICKS = 20L;

    private final MessageBook messages;
    private final NoticeBoard notices;
    private final PluginLog log;
    private volatile MissingReason reported;
    private volatile String presetName = "";

    public MissingPresetNotices(MessageBook messages, NoticeBoard notices, PluginLog log) {
        this.messages = messages;
        this.notices = notices;
        this.log = log;
    }

    public synchronized void report(MissingReason reason, String configuredName) {
        if (!reason.notifies() || reason == reported) {
            return;
        }
        reported = reason;
        presetName = configuredName;
        List<String> raw = rawLines();
        raw.forEach(line -> log.warn(LogTopic.PRESET, messages.strip(line)));
        notices.broadcast(Audience.ADMIN_OR_OP, chatLines(raw));
    }

    public synchronized void forget() {
        reported = null;
    }

    public Optional<MissingReason> current() {
        return Optional.ofNullable(reported);
    }

    public void greet(UUID player) {
        if (reported != null) {
            notices.sendLater(player, Audience.ADMIN_OR_OP, chatLines(rawLines()), JOIN_DELAY_TICKS);
        }
    }

    private List<String> rawLines() {
        List<String> lines = new ArrayList<>();
        MissingReason reason = reported;
        if (reason == null) {
            return lines;
        }
        TemplateValues values = TemplateValues.of("{PRESET}", presetName);
        lines.add(messages.rawText(MissingReason.HEADLINE_KEY));
        reason.detailKey().ifPresent(key -> lines.add(values.apply(messages.rawText(key))));
        return lines;
    }

    private List<String> chatLines(List<String> raw) {
        return raw.stream().map(line -> messages.colorize(messages.rawPrefix() + line)).toList();
    }
}
