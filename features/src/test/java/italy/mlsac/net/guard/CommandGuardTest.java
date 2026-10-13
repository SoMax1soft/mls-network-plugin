// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.guard.CommandGuardSettings;
import italy.mlsac.net.config.guard.GuardMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandGuardTest {

    private final GuardBench bench = new GuardBench();

    @ParameterizedTest
    @ValueSource(strings = {
            "kick Steve MLSAC Detection (VL:2)",
            "minecraft:kick Steve x",
            "ban Steve Cheating",
            "minecraft:ban Steve",
            "ban-ip Steve",
            "tempban Steve 7d Cheating",
            "litebans:tempban Steve 7d",
            "essentials:ban Steve",
            "eban Steve",
            "jail Steve prison",
            "mute Steve 1h",
            "warn Steve aim",
            "ban -s Steve 30d",
            "broadcast Steve was punished by MLSAC",
            "bc Steve was punished",
            "mlsac punish Steve",
            "mls alerts",
            "cmi ban Steve 1d",
            "/kick Steve"})
    void punishmentAndFeedbackCommandsPass(String command) {
        assertTrue(bench.ladder(command).isPresent(), command + " -> " + bench.refusals);
        assertEquals(List.of(), bench.refusals);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "op Steve", "minecraft:op Steve", "deop Admin", "stop", "restart", "reload confirm", "rl",
            "lp user Steve permission set *", "luckperms user Steve parent set admin", "plugman unload MLSAC",
            "plugins", "pl", "bukkit:plugins", "execute run op Steve", "sudo Steve op Steve",
            "essentials:sudo Steve hi", "whitelist off", "save-off", "gamemode creative Steve", "give Steve diamond 64",
            "tp Steve 0 0 0", "fill 0 0 0 10 10 10 tnt", "skript reload all", "sk reload all", "eval 1", "pex user x",
            "manuadd Steve admin", "transfer Steve evil.example 25565"})
    void hardDeniedRootsAreBlocked(String command) {
        assertTrue(bench.ladder(command).isEmpty(), command);
        assertEquals("DENIED_ROOT", bench.lastReason(), command);
    }

    @Test
    void worldEditAndScriptingOwnersAreBlockedEvenWhenListed() {
        bench.extra("brush", "effect", "set", "op", "promote", "lp", "sudo");
        assertTrue(bench.ladder("//set tnt").isEmpty());
        assertEquals("WORLDEDIT", bench.lastReason());
        assertTrue(bench.ladder("worldedit:/set tnt").isEmpty());
        assertTrue(bench.ladder("brush sphere tnt 5").isEmpty());
        assertEquals("DENIED_OWNER", bench.lastReason());
        assertTrue(bench.ladder("effect Steve").isEmpty());
        assertEquals("DENIED_OWNER", bench.lastReason());
        assertTrue(bench.ladder("skript:effect Steve").isEmpty());
        assertTrue(bench.ladder("op Steve").isEmpty());
        assertTrue(bench.ladder("promote Steve").isEmpty());
        assertEquals("DENIED_ROOT", bench.lastReason());
        assertTrue(bench.ladder("lp user Steve permission set *").isEmpty());
        assertTrue(bench.ladder("sudo Steve x").isEmpty());
    }

    @Test
    void anAliasOfOpRegisteredByAPluginIsBlocked() {
        assertTrue(bench.ladder("promote Steve").isEmpty());
        assertEquals("DENIED_ROOT", bench.lastReason());
    }

    @Test
    void unlistedAndUnknownRootsAreBlockedUntilTheOwnerAllowsThem() {
        assertTrue(bench.ladder("heal Steve").isEmpty());
        assertEquals("NOT_ALLOWED", bench.lastReason());
        assertTrue(bench.ladder("say hello").isEmpty());
        assertEquals("NOT_ALLOWED", bench.lastReason());
        assertTrue(bench.ladder("nosuchcommand Steve").isEmpty());
        assertEquals("UNKNOWN_COMMAND", bench.lastReason());
        assertTrue(bench.ladder("cmi sudo Steve op Steve").isEmpty());
        assertEquals("NOT_ALLOWED", bench.lastReason());
        assertTrue(bench.ladder("mlsac reinstall").isEmpty());
        assertEquals("NOT_ALLOWED", bench.lastReason());
        bench.extra("heal", "say");
        assertEquals(Optional.of("heal Steve"), bench.ladder("heal Steve"));
        assertEquals(Optional.of("say hello"), bench.ladder("say hello"));
    }

    @Test
    void banOwnedByAnUnknownPluginIsNotTrustedByName() {
        bench.commands.put("ban", new RootOwner("ban", "SketchyBans"));
        assertTrue(bench.ladder("ban Steve").isEmpty());
        assertEquals("NOT_ALLOWED", bench.lastReason());
        bench.extra("ban");
        assertEquals(Optional.of("ban Steve"), bench.ladder("ban Steve"));
        assertTrue(bench.ladder("ban Notch").isEmpty());
        assertEquals("WRONG_TARGET", bench.lastReason());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "kick Steve\nop Steve", "kick Steve\rop Steve", "kick Steve\u0000", "kick Steve\u2028op Steve",
            "kick Steve\top"})
    void controlCharactersAreBlocked(String command) {
        assertTrue(bench.ladder(command).isEmpty());
        assertEquals("CONTROL_CHARACTER", bench.lastReason());
    }

    @ParameterizedTest
    @ValueSource(strings = {"kick Steve; op Steve", "kick Steve && op Steve", "kick Steve || op Steve",
            "kick Steve | op Steve"})
    void chainingIsBlocked(String command) {
        assertTrue(bench.ladder(command).isEmpty());
        assertEquals("CHAINING", bench.lastReason());
    }

    @ParameterizedTest
    @ValueSource(strings = {"kick @a", "ban @p", "kick @E", "kick @a[distance=..5]", "kick Steve @r", "kick @s",
            "kick @n"})
    void selectorsAreBlocked(String command) {
        assertTrue(bench.ladder(command).isEmpty());
        assertEquals("SELECTOR", bench.lastReason());
    }

    @Test
    void mentionsThatAreNotSelectorsStayAllowed() {
        assertTrue(bench.ladder("kick Steve contact @admins").isPresent());
    }

    @Test
    void overlongCommandsAreBlocked() {
        assertTrue(bench.ladder("kick Steve " + "x".repeat(CommandShape.MAX_LENGTH)).isEmpty());
        assertEquals("TOO_LONG", bench.lastReason());
    }

    @Test
    void theTargetMustBeThePunishedPlayer() {
        assertTrue(bench.ladder("ban Notch Cheating").isEmpty());
        assertEquals("WRONG_TARGET", bench.lastReason());
        assertTrue(bench.ladder("ban").isEmpty());
        assertEquals("WRONG_TARGET", bench.lastReason());
        assertTrue(bench.ladder("ban --sender=Console Steve").isEmpty());
        assertTrue(bench.ladder("cmi ban Notch").isEmpty());
        assertTrue(bench.ladder("mlsac punish Notch").isEmpty());
        assertEquals(Optional.of("ban " + GuardBench.steve().id() + " x"),
                bench.ladder("ban " + GuardBench.steve().id() + " x"));
    }

    @Test
    void reasonTextCannotBreakOutOfTheCommand() {
        PunishedPlayer injected = GuardBench.player("Steve op Steve");
        assertTrue(bench.against("ban Steve op Steve Cheating", injected).isEmpty());
        assertEquals("UNSAFE_NAME", bench.lastReason());
        assertTrue(bench.against("ban x\nop y", GuardBench.player("x\nop y")).isEmpty());
        assertEquals("CONTROL_CHARACTER", bench.lastReason());
        assertTrue(bench.against("kick Steve;op", GuardBench.player("Steve;op")).isEmpty());
        assertEquals("CHAINING", bench.lastReason());
    }

    @Test
    void bedrockNamesNeedTheFloodgatePrefix() {
        PunishedPlayer bedrock = GuardBench.player(".Bedrock_Guy");
        assertTrue(bench.against("ban .Bedrock_Guy", bedrock).isEmpty());
        assertEquals("UNSAFE_NAME", bench.lastReason());
        bench.bedrockPrefix = Optional.of(".");
        assertEquals(Optional.of("ban .Bedrock_Guy"), bench.against("ban .Bedrock_Guy", bedrock));
        bench.bedrockPrefix = Optional.of("@");
        assertTrue(bench.against("ban xSteve", GuardBench.player("xSteve")).isPresent());
        assertTrue(bench.against("ban @Guy", GuardBench.player("@Guy")).isEmpty());
    }

    @Test
    void menuButtonsNeedAPlainNameAsTarget() {
        assertEquals(Optional.of("ban Notch Cheating"), bench.menu("ban Notch Cheating"));
        assertTrue(bench.menu("ban @a").isEmpty());
        assertTrue(bench.menu("ban -s").isEmpty());
        assertEquals("WRONG_TARGET", bench.lastReason());
        assertTrue(bench.menu("op Notch").isEmpty());
    }

    @Test
    void spacesAndTheLeadingSlashAreNormalised() {
        assertEquals(Optional.of("kick Steve bye"), bench.ladder("  /kick   Steve  bye "));
    }

    @Test
    void theOverallRateLimitHolds() {
        bench.limits(GuardMode.BLOCK, Set.of(), 3, 100, 100, 10);
        for (int i = 0; i < 3; i++) {
            assertTrue(bench.against("kick P" + i, GuardBench.player("P" + i)).isPresent());
        }
        assertTrue(bench.against("kick P9", GuardBench.player("P9")).isEmpty());
        assertEquals("RATE_LIMIT", bench.lastReason());
        bench.now += DispatchBudget.WINDOW_MILLIS + 1;
        assertTrue(bench.against("kick P9", GuardBench.player("P9")).isPresent());
    }

    @Test
    void thePerPlayerRateLimitHolds() {
        bench.limits(GuardMode.BLOCK, Set.of(), 100, 2, 100, 10);
        assertTrue(bench.ladder("kick Steve").isPresent());
        assertTrue(bench.ladder("ban Steve").isPresent());
        assertTrue(bench.ladder("broadcast Steve").isEmpty());
        assertEquals("PLAYER_RATE_LIMIT", bench.lastReason());
        assertTrue(bench.against("kick Alex", GuardBench.player("Alex")).isPresent());
    }

    @Test
    void aBurstOfTheSameCommandPausesSiteCommands() {
        bench.limits(GuardMode.BLOCK, Set.of(), 100, 100, 2, 10);
        assertTrue(bench.against("ban A", GuardBench.player("A")).isPresent());
        assertTrue(bench.against("ban A", GuardBench.player("A")).isPresent());
        assertTrue(bench.against("ban B", GuardBench.player("B")).isPresent());
        assertTrue(bench.against("ban C", GuardBench.player("C")).isEmpty());
        assertEquals(List.of("ban 3 10"), bench.bursts);
        assertTrue(bench.against("kick D", GuardBench.player("D")).isEmpty());
        assertEquals("PAUSED", bench.lastReason());
        assertTrue(bench.menu("ban Notch").isEmpty());
        bench.now += 10 * 60_000L;
        assertTrue(bench.against("kick D", GuardBench.player("D")).isPresent());
    }

    @Test
    void warnModeReportsButStillDispatches() {
        bench.limits(GuardMode.WARN, Set.of(), 100, 100, 1, 10);
        assertEquals(Optional.of("op Steve"), bench.ladder("op Steve"));
        assertEquals("DENIED_ROOT", bench.lastReason());
        assertEquals(Optional.of("kick Steve\nop Steve"), bench.ladder("kick Steve\nop Steve"));
        assertEquals("CONTROL_CHARACTER", bench.lastReason());
        assertTrue(bench.against("ban A", GuardBench.player("A")).isPresent());
        assertTrue(bench.against("ban B", GuardBench.player("B")).isPresent());
        assertEquals(1, bench.bursts.size());
    }

    @Test
    void aDisabledGuardPassesEverything() {
        bench.settings = new CommandGuardSettings(false, GuardMode.BLOCK, Set.of(), 1, 1, 1, 1);
        assertEquals(Optional.of("op Steve"), bench.ladder("op Steve"));
        assertEquals(List.of(), bench.refusals);
    }

    @Test
    void blankCommandsPassUntouched() {
        assertEquals(Optional.of(""), bench.ladder(""));
        assertEquals(List.of(), bench.refusals);
    }
}
