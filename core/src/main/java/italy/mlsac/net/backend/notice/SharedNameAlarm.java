// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.notice;

import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.config.core.ServerIdentity;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.player.Audience;
import italy.mlsac.net.player.NoticeBoard;
import italy.mlsac.net.text.LegacyColorizer;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SharedNameAlarm {

    private static final String DEFAULT_ADVICE = "Another server on this network also calls itself 'default'; "
            + "give this one its own server-identity.name in config.yml";

    private final Supplier<ServerIdentity> identity;
    private final NoticeThrottle throttle;
    private final List<Consumer<String>> outlets;

    SharedNameAlarm(Supplier<ServerIdentity> identity, NoticeThrottle throttle, List<Consumer<String>> outlets) {
        this.identity = identity;
        this.throttle = throttle;
        this.outlets = List.copyOf(outlets);
    }

    public static SharedNameAlarm toConsoleAndStaff(Supplier<ServerIdentity> identity, PluginLog log,
                                                    NoticeBoard notices, LegacyColorizer colorizer, Clock clock) {
        Consumer<String> console = text -> log.warn(LogTopic.BACKEND, text);
        Consumer<String> staff = text -> notices.broadcast(Audience.STAFF_OR_OP,
                List.of(colorizer.colorize("&c! &f" + text)));
        NoticeThrottle throttle = new NoticeThrottle(3, 30_000L, () -> clock.wallMillis("notice.duplicate_name"));
        return new SharedNameAlarm(identity, throttle, List.of(console, staff));
    }

    public void inspect(JsonObject reply) {
        DuplicateNameNotice notice = DuplicateNameNotice.readFrom(reply);
        ServerIdentity local = identity.get();
        boolean concernsUs = notice.active() && local.interServer() && local.usesDefaultName();
        if (!concernsUs) {
            throttle.endStreak();
        } else if (throttle.mayShow()) {
            String text = notice.textOr(DEFAULT_ADVICE);
            outlets.forEach(outlet -> outlet.accept(text));
        }
    }
}
