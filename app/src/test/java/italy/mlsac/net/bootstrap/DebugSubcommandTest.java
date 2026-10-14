// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.testkit.FakeActor;
import italy.mlsac.net.testkit.FakePlayers;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.text.LanguageId;
import italy.mlsac.net.text.MessageBook;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DebugSubcommandTest {

    private static final Pattern RAW_CODE = Pattern.compile("[&][0-9a-fk-orxA-FK-ORX#]");

    private final RecordingReport report = new RecordingReport();
    private final FakePlayers players = new FakePlayers();
    private final DebugSubcommand debug = new DebugSubcommand(report, players, messagesIn(LanguageId.EN));

    @AfterEach
    void releaseDebugSwitch() {
        DebugTally.force(null);
        DebugTally.traceTarget(null, null);
    }

    @Test
    void switchRepliesCarryColoursInsteadOfAmpersandCodes() {
        FakeActor console = FakeActor.console();
        debug.execute(console, List.of("on"));
        assertEquals(Boolean.TRUE, DebugTally.forced());
        debug.execute(console, List.of("off"));
        assertEquals(Boolean.FALSE, DebugTally.forced());
        debug.execute(console, List.of("auto"));
        assertEquals(null, DebugTally.forced());
        List<String> replies = console.received();
        assertEquals(3, replies.size());
        assertTrue(replies.get(0).startsWith("§aMLSAC debug ON"), replies.get(0));
        assertTrue(replies.get(1).startsWith("§eMLSAC debug OFF"), replies.get(1));
        assertTrue(replies.get(2).startsWith("§7MLSAC debug follows config.yml"), replies.get(2));
        replies.forEach(reply -> assertFalse(RAW_CODE.matcher(reply).find(), reply));
        assertEquals(3, report.refreshes);
    }

    @Test
    void statusLinesAndPlayerTraceRepliesAreColoured() {
        FakeActor console = FakeActor.console();
        players.add("Legit");
        debug.execute(console, List.of());
        debug.execute(console, List.of("status"));
        debug.execute(console, List.of("player", "Nobody"));
        debug.execute(console, List.of("player", "Legit"));
        debug.execute(console, List.of("player", "off"));
        assertEquals(List.of("§7Players online 1", "§7Ticks: server 200", "§7Players online 1", "§7Ticks: server 200",
                "§cPlayer Nobody is not online.",
                "§aTracing Legit: every hit and backend answer goes to the console.", "§7Player trace off."),
                console.received());
    }

    @Test
    void everyReplyInEveryLanguageIsTranslatedAndColoured() {
        for (LanguageId language : LanguageId.values()) {
            DebugSubcommand localised = new DebugSubcommand(report, players, messagesIn(language));
            players.add("Legit" + language.id());
            FakeActor console = FakeActor.console();
            for (List<String> args : List.of(List.of("on"), List.of("off"), List.of("auto"), List.of("status"),
                    List.of("player", "Nobody"), List.of("player", "Legit" + language.id()), List.of("player"))) {
                localised.execute(console, args);
            }
            assertEquals(8, console.received().size(), language.id());
            for (String reply : console.received()) {
                assertFalse(RAW_CODE.matcher(reply).find(), language.id() + ": " + reply);
                assertFalse(reply.contains("No text"), language.id() + ": " + reply);
                assertTrue(reply.startsWith("§"), language.id() + ": " + reply);
            }
            assertTrue(console.received().stream().anyMatch(reply -> reply.contains("Nobody")), language.id());
            assertTrue(console.received().stream().anyMatch(reply -> reply.contains("Legit" + language.id())),
                    language.id());
        }
    }

    private static MessageBook messagesIn(LanguageId language) {
        return TestConfigs.messages(new ConfigReference(EffectiveConfig.pluginDefaults(
                TestConfigs.core("language: " + language.id()))));
    }

    private static final class RecordingReport implements DebugReport {

        private int refreshes;

        @Override
        public void refreshNow() {
            refreshes++;
        }

        @Override
        public List<String> summary() {
            return List.of("Players online 1", "Ticks: server 200");
        }
    }
}
