// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.player.Audience;
import italy.mlsac.net.player.NoticeBoard;
import italy.mlsac.net.staff.StaffChannel;
import italy.mlsac.net.suppression.server.SuppressionNotices;
import italy.mlsac.net.text.MessageBook;
import java.util.List;

public record StaffNoticeRelay(StaffChannel staff, NoticeBoard notices, MessageBook messages, PluginLog log)
        implements SuppressionNotices {

    @Override
    public void toAlertSubscribers(String text) {
        staff.toAlertSubscribers(messages.colorize(text));
    }

    @Override
    public void toStaffAndOps(String text) {
        notices.broadcast(Audience.STAFF_OR_OP, List.of(messages.colorize(text)));
    }

    @Override
    public void toConsole(String text) {
        log.info(LogTopic.GENERAL, messages.strip(messages.colorize(text)));
    }

    @Override
    public void warn(String line) {
        log.warn(LogTopic.GENERAL, line);
    }
}
