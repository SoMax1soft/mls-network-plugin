// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import italy.mlsac.net.bus.SynchronousEventBus;
import italy.mlsac.net.config.core.ServerIdentity;
import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.json.JsonParser;
import italy.mlsac.net.json.JsonSyntaxException;
import italy.mlsac.net.player.NoticeBoard;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.FakePlayer;
import italy.mlsac.net.testkit.FakePlayers;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.ManualScheduler;
import italy.mlsac.net.testkit.TestLogs;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.text.LegacyColorizer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReplyObserversTest {

    private final ManualClock clock = new ManualClock(0);
    private final CapturingLogSink sink = new CapturingLogSink();
    private final ConsoleLog log = TestLogs.capturing(sink, clock, false);

    @Test
    void interServerEventsAreDedupedSanitisedAndReportsDropped() throws JsonSyntaxException {
        SynchronousEventBus bus = new SynchronousEventBus(log);
        List<InterServerAlert> received = new ArrayList<>();
        bus.subscribe(InterServerAlert.class, received::add);
        InterServerFeed feed = new InterServerFeed(bus);
        String longName = "a".repeat(40);
        String reply = "{\"success\":true,\"data\":{\"interserverEvents\":["
                + "{\"id\":\"ev-1\",\"type\":\"alert\",\"serverName\":\"" + longName + "\",\"playerName\":\"Re\\u0001mote\","
                + "\"model\":\"ultra\",\"probability\":0.91,\"buffer\":12.5,\"vl\":1},"
                + "{\"id\":\"ev-1\",\"type\":\"alert\"},"
                + "{\"id\":\"ev-2\",\"type\":\"alert\",\"reason\":\"spam\"},"
                + "{\"id\":\"ev-3\",\"type\":\"player-report\"},"
                + "5,"
                + "{\"type\":\"punish\",\"action\":\"kick\",\"violationLevel\":2.6}]}}";
        feed.accept(JsonParser.parseObject(reply));
        assertEquals(2, received.size());
        InterServerAlert first = received.get(0);
        assertEquals("a".repeat(32), first.hostLabel());
        assertEquals("Remote", first.playerNick());
        assertEquals(1, first.offenceLevel());
        InterServerAlert second = received.get(1);
        assertEquals("unknown", second.hostLabel());
        assertEquals("Unknown", second.playerNick());
        assertEquals(3, second.offenceLevel());
        assertEquals("kick", second.action());
        feed.accept(JsonParser.parseObject(reply));
        assertEquals(3, received.size());
    }

    @Test
    void duplicateNameWarningsAreLimitedPerSeries() throws JsonSyntaxException {
        ManualScheduler scheduler = new ManualScheduler();
        FakePlayers players = new FakePlayers();
        FakePlayer op = players.add("Op").asOp();
        ServerIdentity identity = new ServerIdentity("default", "default", true, true, 0.8);
        SharedNameAlarm watch = SharedNameAlarm.toConsoleAndStaff(() -> identity, log, new NoticeBoard(players, scheduler),
                new LegacyColorizer(new HexColorNormalizer()), clock);
        String active = "{\"warnings\":{\"duplicateServerName\":{\"active\":true,\"message\":\"rename me\"}}}";
        for (int i = 0; i < 6; i++) {
            watch.inspect(JsonParser.parseObject(active));
            clock.advanceMillis(10_000);
        }
        scheduler.runPending();
        assertEquals(1, sink.texts(LogLevel.WARNING).size());
        clock.advanceMillis(60_000);
        watch.inspect(JsonParser.parseObject(active));
        watch.inspect(JsonParser.parseObject("{\"warnings\":{\"duplicateServerName\":{\"active\":false}}}"));
        clock.advanceMillis(30_000);
        watch.inspect(JsonParser.parseObject(active));
        scheduler.runPending();
        assertEquals(4, op.messages().size());
        assertEquals("§c! §frename me", op.messages().get(0));
    }
}
