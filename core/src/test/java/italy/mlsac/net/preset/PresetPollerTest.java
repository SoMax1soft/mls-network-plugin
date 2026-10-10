// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.preset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.config.ConfigAssembler;
import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.PresetLayout;
import italy.mlsac.net.config.PresetSnapshot;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.config.core.PresetSelection;
import italy.mlsac.net.console.ConsoleLog;
import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.player.NoticeBoard;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.testkit.FakePlayer;
import italy.mlsac.net.testkit.FakePlayers;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.ManualScheduler;
import italy.mlsac.net.testkit.ScriptedTransport;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.testkit.TestLogs;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PresetPollerTest {

    private static final String FOUND = "{\"success\":true,\"data\":{\"preset\":\"pvp\",\"revision\":7,\"hash\":\"abc\","
            + "\"format\":2,\"config\":{\"alerts\":{\"threshold\":0.7}}}}";

    private final ManualScheduler scheduler = new ManualScheduler();
    private final ManualClock clock = new ManualClock(0);
    private final ScriptedTransport transport = new ScriptedTransport();
    private final CapturingLogSink sink = new CapturingLogSink();
    private final ConsoleLog log = TestLogs.capturing(sink, clock, false);
    private final FakePlayers players = new FakePlayers();
    private final List<Optional<PresetSnapshot>> applied = new ArrayList<>();
    private final ConfigReference reference = new ConfigReference(TestConfigs.defaults());
    private final MissingPresetNotices notices = new MissingPresetNotices(TestConfigs.messages(reference),
            new NoticeBoard(players, scheduler), log);
    private final PresetPoller poller = new PresetPoller(scheduler, new PresetFetcher(transport, "http://inference.test"),
            new PresetLayout(), new ConfigAssembler(), applied::add, notices, log, clock);

    @Test
    void readerMapsEveryOutcome() {
        PresetAnswerReader reader = new PresetAnswerReader();
        assertInstanceOf(PresetAnswer.Delivered.class, reader.read(HttpReply.of(200, FOUND)));
        assertEquals(new PresetAnswer.Unavailable(MissingReason.MULTIPLE_PRESETS), reader.read(HttpReply.of(200,
                "{\"success\":true,\"data\":{\"preset\":null,\"config\":null,\"reason\":\"MULTIPLE_PRESETS\"}}")));
        assertEquals(new PresetAnswer.Unavailable(MissingReason.NOTHING_USABLE),
                reader.read(HttpReply.of(200, "{\"data\":{\"config\":[]}}")));
        assertEquals(new PresetAnswer.Unavailable(MissingReason.NO_ENDPOINT), reader.read(HttpReply.of(404, "")));
        assertEquals(new PresetAnswer.Failed("HTTP 403", true), reader.read(HttpReply.of(403, "{}")));
        assertEquals(new PresetAnswer.Failed("HTTP 429", false), reader.read(HttpReply.of(429, "{}")));
        assertInstanceOf(PresetAnswer.Failed.class, reader.read(HttpReply.of(200, "{\"data\":5}")));
        assertInstanceOf(PresetAnswer.Failed.class, reader.read(HttpReply.of(200, "<html>")));
    }

    @Test
    void urlCarriesOnlyNamedPresets() {
        PresetFetcher fetcher = new PresetFetcher(transport, "https://api.mlsac.net");
        assertEquals("https://api.mlsac.net/api/v1/plugin/config",
                fetcher.uri(PresetSelection.auto()).toString());
        assertEquals("https://api.mlsac.net/api/v1/plugin/config?preset=pvp-1",
                fetcher.uri(PresetSelection.parse("pvp-1").selection()).toString());
    }

    @Test
    void pollsAfterTwoSecondsThenEveryMinuteAndAppliesOnlyChanges() {
        transport.reply("/plugin/config", 200, FOUND).reply("/plugin/config", 200, FOUND);
        poller.start(core("pvp"), true);
        scheduler.advance(39);
        assertEquals(0, transport.calls().size());
        scheduler.advance(1);
        assertEquals(1, transport.calls().size());
        scheduler.runPending();
        assertEquals(1, applied.size());
        assertEquals("abc", applied.get(0).orElseThrow().hash());
        scheduler.advance(1200);
        assertEquals(2, transport.calls().size());
        assertEquals(1, applied.size());
        assertEquals(List.of("X-API-Key", "X-MLSAC-Plugin-Generation"),
                transport.calls().get(0).call().headers().stream().map(h -> h.name()).toList());
        assertTrue(sink.contains("Applying preset 'pvp' (abc)"));
    }

    @Test
    void missingPresetIsReportedOncePerReasonAndDropsTheAppliedPreset() {
        FakePlayer admin = players.add("Admin").asOp();
        transport.reply("/plugin/config", 200, FOUND);
        String notFound = "{\"success\":true,\"data\":{\"preset\":null,\"config\":null,\"reason\":\"PRESET_NOT_FOUND\"}}";
        transport.reply("/plugin/config", 200, notFound).reply("/plugin/config", 200, notFound);
        poller.start(core("pvp"), true);
        scheduler.advance(40);
        scheduler.advance(1200);
        scheduler.advance(1200);
        assertEquals(2, applied.size());
        assertTrue(applied.get(1).isEmpty());
        assertEquals(2, sink.texts(LogLevel.WARNING).size());
        assertEquals(2, admin.messages().size());
        assertTrue(admin.messages().get(1).contains("pvp"));
    }

    @Test
    void failedFetchKeepsWhateverIsApplied() {
        transport.reply("/plugin/config", 200, FOUND).reply("/plugin/config", 500, "{}");
        poller.start(core("pvp"), true);
        scheduler.advance(40);
        scheduler.advance(1200);
        assertEquals(1, applied.size());
        assertTrue(sink.texts(LogLevel.WARNING).isEmpty());
    }

    @Test
    void sentinelNeverPollsAndReportsDisabled() {
        poller.start(core("none"), true);
        scheduler.advance(5000);
        assertEquals(0, transport.calls().size());
        assertEquals(Optional.of(MissingReason.DISABLED), notices.current());
    }

    private static CoreSettings core(String preset) {
        return TestConfigs.core("detection:\n  api-key: real-key-123\nremote-config:\n  preset: " + preset + "\n");
    }
}
