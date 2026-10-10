// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.text.MessageBook;
import italy.mlsac.net.text.TemplateValues;
import italy.mlsac.net.scoring.SuspectRecord;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandTest {

    private final CommandHarness harness = new CommandHarness();
    private final MessageBook messages = harness.messages;
    private final FakeInvoker admin = FakeInvoker.player("Admin", "mlsac.admin", "mlsac.alerts");
    private Locale previous;

    @BeforeEach
    void locale() {
        previous = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.ROOT);
    }

    @AfterEach
    void restore() {
        Locale.setDefault(Locale.Category.FORMAT, previous);
    }

    @Test
    void dispatchOrderPlayersOnlyBeforePermission() {
        assertEquals(List.of(messages.prefixed("players-only")), harness.run(FakeInvoker.console(), "alerts"));
        FakeInvoker nobody = FakeInvoker.player("Nobody");
        assertEquals(List.of(messages.prefixed("no-permission")), harness.run(nobody, "alerts"));
        List<String> unknown = harness.run(admin, "Foo");
        assertEquals(messages.prefixed("unknown-command", TemplateValues.of("{ARGS}", "Foo")), unknown.get(0));
        assertEquals(messages.prefixed("usage-header"), unknown.get(1));
        assertEquals(List.of(messages.prefixed("no-permission")), harness.run(nobody, ""));
    }

    @Test
    void usageListShowsOnlyAllowedLinesInOrder() {
        FakeInvoker staff = FakeInvoker.player("Staff", "mlsac.alerts");
        List<String> lines = harness.run(staff, "");
        assertEquals(List.of(messages.prefixed("usage-header"), messages.text("usage-alerts"),
                messages.text("help-monitor"),
                messages.text("usage-suspects")), lines);
    }

    @Test
    void tabCompletionForAlertsNodeOnly() {
        FakeInvoker staff = FakeInvoker.player("Staff", "mlsac.alerts");
        assertEquals(List.of("alerts", "monitor", "suspects"), harness.command.complete(staff, List.of("")));
        assertEquals(List.of("monitor"), harness.command.complete(staff, List.of("MO")));
        assertEquals(List.of(), harness.command.complete(staff, List.of("punish", "")));
        harness.join("Steve");
        assertEquals(List.of("chat", "prob"), harness.command.complete(staff, List.of("monitor", "")));
        assertEquals(List.of("Steve"), harness.command.complete(staff, List.of("monitor", "prob", "st")));
        assertEquals(List.of("confirm"), harness.command.complete(admin, List.of("punish", "Steve", "")));
        assertEquals(List.of("restore"), harness.command.complete(admin, List.of("falsepositive", "zzz")));
    }

    @Test
    void alertsToggle() {
        FakeInvoker staff = FakeInvoker.player("Staff", "mlsac.alerts");
        assertEquals(List.of(messages.prefixed("alerts-enabled")), harness.run(staff, "alerts"));
        assertEquals(List.of(messages.prefixed("alerts-disabled")), harness.run(staff, "alerts"));
    }

    @Test
    void punishNeedsEvidenceOrConfirm() {
        UUID steve = harness.join("Steve");
        assertEquals(List.of(messages.prefixed("punish-no-evidence", TemplateValues.of("{PLAYER}", "Steve")),
                messages.text("punish-confirm-hint", TemplateValues.of("{PLAYER}", "Steve"))),
                harness.run(admin, "punish Steve"));
        assertTrue(harness.console.isEmpty());
        assertEquals(List.of(messages.prefixed("punish-success", TemplateValues.of("{PLAYER}", "Steve")
                .and("{ACTION}", "Max VL"))), harness.run(admin, "punish steve CONFIRM"));
        assertEquals(List.of("ban Steve 3"), harness.console);
        assertEquals(0, harness.ledger.level(steve));
        assertEquals(List.of(messages.prefixed("player-not-found", TemplateValues.of("{PLAYER}", "Ste"))),
                harness.run(admin, "punish Ste"));
    }

    @Test
    void punishWithEvidenceRunsDirectly() {
        UUID steve = harness.join("Steve");
        harness.ledger.raise(steve);
        harness.run(admin, "punish Steve");
        assertEquals(List.of("ban Steve 3"), harness.console);
        harness.rules = new italy.mlsac.net.punishment.PenaltyRules(italy.mlsac.net.punishment.LadderRungs.of(
                java.util.Map.of()), true, "classic_ban", false, false);
        assertEquals(List.of(messages.prefixed("punish-no-action")), harness.run(admin, "punish Steve"));
    }

    @Test
    void monitorProbStartsAndStopsTracking() {
        UUID steve = harness.join("Steve");
        FakeInvoker staff = FakeInvoker.player("Staff", "mlsac.alerts");
        harness.online.put("Staff", staff.actorUuid().orElseThrow());
        assertEquals(List.of(messages.prefixed("tracking-started", TemplateValues.of("{PLAYER}", "Steve"))),
                harness.run(staff, "monitor prob Ste"));
        assertEquals(List.of(messages.text("monitor-no-data", TemplateValues.of("{PLAYER}", "Steve"))),
                harness.actionBars);
        SuspectRecord record = harness.records.get(steve);
        record.count("ultra", 0.953, before -> 13.2);
        harness.ledger.raise(steve);
        assertTrue(harness.repeating.get(0).getAsBoolean());
        assertEquals(messages.colorize("&f&l● 95.3% &7| &e▲ 13.2 &7| &bvl 1"), harness.actionBars.get(1));
        assertEquals(List.of(messages.prefixed("tracking-stopped")), harness.run(staff, "monitor prob Whatever"));
        assertEquals(false, harness.repeating.get(0).getAsBoolean());
        assertEquals(List.of(messages.prefixed("prob-usage")), harness.run(staff, "monitor prob"));
        assertEquals(List.of(messages.prefixed("monitor-usage")), harness.run(staff, "monitor what"));
        assertEquals(List.of(messages.prefixed("monitor-enabled")), harness.run(staff, "monitor"));
        assertEquals(List.of(messages.prefixed("monitor-disabled")), harness.run(staff, "monitor chat"));
    }

    @Test
    void trackingStopsWhenTargetLeaves() {
        UUID steve = harness.join("Steve");
        FakeInvoker staff = FakeInvoker.player("Staff", "mlsac.alerts");
        harness.online.put("Staff", staff.actorUuid().orElseThrow());
        harness.run(staff, "monitor prob Steve");
        harness.online.remove("Steve");
        assertEquals(false, harness.repeating.get(0).getAsBoolean());
        assertEquals(messages.text("player-offline"), harness.actionBars.get(harness.actionBars.size() - 1));
        assertTrue(steve != null);
    }

    @Test
    void reinstallNeedsConfirmationWithinThreeSeconds() {
        String prompt = messages.prefixed("reinstall-confirm");
        assertEquals(List.of(prompt), harness.run(admin, "reinstall"));
        harness.now += 2_000;
        assertEquals(List.of(messages.prefixed("reinstall-done")), harness.run(admin, "reinstall"));
        assertEquals(List.of(prompt), harness.run(admin, "reinstall"));
        harness.now += 4_000;
        assertEquals(List.of(prompt), harness.run(admin, "reinstall"));
    }

    @Test
    void kicklistPagesAndClamps() {
        assertEquals(List.of(messages.prefixed("kicklist-empty")), harness.run(admin, "kicklist"));
        UUID steve = harness.join("Steve");
        harness.ledger.raise(steve);
        for (int i = 0; i < 3; i++) {
            harness.run(admin, "punish Steve");
        }
        List<String> out = harness.run(admin, "kicklist 5");
        assertEquals(messages.prefixed("kicklist-header", TemplateValues.of("{PAGE}", 1).and("{MAX_PAGE}", 1)), out.get(0));
        assertEquals(6, out.size());
        assertEquals(messages.colorize("&e1. &fSteve &7[&c10:00:00&7] &8- &bProb: &f1.00 &8| &bBuf: &f100.0 &8| &bVL: &f3"),
                out.get(2));
    }

    @Test
    void reloadReportsAnimationsThenConfig() {
        List<String> out = harness.run(admin, "reload");
        assertEquals(List.of(messages.prefixed("animations-reloaded", TemplateValues.of("{COUNT}", 3)),
                messages.prefixed("config-reloaded")), out);
        assertEquals(1, harness.reloads);
    }

    @Test
    void profileUsesListMessage() {
        UUID steve = harness.join("Steve");
        harness.records.get(steve).count("ultra", 0.95, before -> 1);
        List<String> out = harness.run(admin, "profile ste");
        assertEquals(messages.text("profile-header", TemplateValues.of("{PLAYER}", "Steve")), out.get(0));
        assertEquals(messages.colorize("&7Sens: &fN/A%"), out.get(1));
        assertEquals(messages.colorize("&7Client: &fV_1_21_4"), out.get(2));
        assertEquals(messages.colorize("&7Detections (>80%): &f1"), out.get(3));
        assertEquals(List.of(messages.prefixed("prob-deprecated")), harness.run(admin, "prob Steve"));
    }

    @Test
    void falsepositiveRestore() {
        UUID steve = harness.join("Steve");
        assertEquals(List.of(messages.prefixed("falsepositive-no-data")), harness.run(admin, "falsepositive restore Steve"));
        harness.records.get(steve).count("ultra", 0.5, before -> 0);
        assertEquals(List.of(messages.prefixed("falsepositive-success")), harness.run(admin, "falsepositive restore Steve"));
        assertEquals(List.of(messages.prefixed("falsepositive-usage")), harness.run(admin, "falsepositive Steve"));
    }
}
