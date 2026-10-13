// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.testkit.FakeActor;
import italy.mlsac.net.testkit.ManualClock;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.testkit.TestLogs;
import italy.mlsac.net.testkit.CapturingLogSink;
import italy.mlsac.net.text.MessageBook;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandRouterTest {

    private final List<String> executed = new ArrayList<>();
    private CommandRouter router;

    @BeforeEach
    void setUp() {
        EffectiveConfig english = EffectiveConfig.pluginDefaults(TestConfigs.core("language: en\n"));
        MessageBook messages = TestConfigs.messages(new ConfigReference(english));
        router = new CommandRouter(messages, TestLogs.capturing(new CapturingLogSink(), new ManualClock(0), false));
        for (SubcommandName name : SubcommandName.values()) {
            router.register(new Subcommand() {
                @Override
                public SubcommandName name() {
                    return name;
                }

                @Override
                public void execute(CommandActor actor, List<String> args) {
                    executed.add(name.id() + args);
                }

                @Override
                public List<String> complete(CommandActor actor, List<String> args) {
                    return List.of("x");
                }
            });
        }
    }

    @Test
    void consoleCannotRunPlayerOnlyCommands() {
        FakeActor console = FakeActor.console();
        router.dispatch(console, List.of("alerts"));
        assertEquals(List.of("§6[MLSAC] §r§cThis command is for players only").size(), console.received().size());
        assertTrue(console.received().get(0).endsWith("This command is for players only"));
        assertTrue(executed.isEmpty());
    }

    @Test
    void playerWithoutNodesIsRefused() {
        FakeActor player = FakeActor.player("Steve");
        router.dispatch(player, List.of("alerts"));
        assertTrue(player.received().get(0).endsWith("You don't have permission"));
        assertTrue(executed.isEmpty());
    }

    @Test
    void unknownSubcommandShowsItsNameAndTheUsage() {
        FakeActor console = FakeActor.console();
        router.dispatch(console, List.of("FooBar"));
        assertTrue(console.received().get(0).endsWith("Unknown command: §fFooBar"));
        assertTrue(console.received().get(1).contains("Commands"));
        assertTrue(console.received().size() > 10);
    }

    @Test
    void permittedCommandRunsWithRemainingArguments() {
        FakeActor player = FakeActor.player("Mod").grant("mlsac.alerts");
        router.dispatch(player, List.of("MONITOR", "prob", "Steve"));
        assertEquals(List.of("monitor[prob, Steve]"), executed);
    }

    @Test
    void tabCompletionOffersOnlyPermittedNamesInOrder() {
        FakeActor player = FakeActor.player("Mod").grant("mlsac.alerts");
        assertEquals(List.of("alerts", "monitor", "suspects", "menu"), router.complete(player, List.of("")));
        assertEquals(List.of("monitor"), router.complete(player, List.of("MO")));
        assertEquals(List.of("x"), router.complete(player, List.of("monitor", "")));
        assertEquals(List.of(), router.complete(player, List.of("reload", "")));
    }

    @Test
    void usageListsOnlyAllowedLines() {
        FakeActor player = FakeActor.player("Mod").grant("mlsac.alerts");
        router.dispatch(player, List.of());
        List<String> lines = player.received();
        assertTrue(lines.get(0).contains("Commands"));
        assertEquals(4, lines.size());
        FakeActor nobody = FakeActor.player("Nobody");
        router.dispatch(nobody, List.of());
        assertEquals(1, nobody.received().size());
        assertTrue(nobody.received().get(0).endsWith("You don't have permission"));
    }
}
