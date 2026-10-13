// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.config.penalty.PenaltyLadder;
import italy.mlsac.net.config.suppression.CommandAction;
import italy.mlsac.net.config.suppression.CommandPreset;
import italy.mlsac.net.punishment.LadderCommand;
import italy.mlsac.net.punishment.LadderRungs;
import italy.mlsac.net.punishment.PenaltyText;
import italy.mlsac.net.scoring.FlagContext;
import italy.mlsac.net.testkit.BundledResources;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BundledCommandsTest {

    private static final List<String> ANIMATIONS = List.of("classic_ban", "dramatic_ban", "rainbow_ban");

    @Test
    void everyBundledDefaultCommandPassesTheGuard() {
        List<String> defaults = new ArrayList<>();
        LadderRungs.pluginDefaults().rungs().values().forEach(defaults::addAll);
        PenaltyLadder.pluginDefaults().rungs().values().forEach(defaults::addAll);
        defaults.add(new CommandAction(0, CommandPreset.KICK, "kick {PLAYER} MLSAC (VL:{VL})", false).ladderText());
        defaults.add(new CommandAction(1, CommandPreset.BAN, "ban {PLAYER} Cheating", true).ladderText());
        defaults.add(new CommandAction(1, CommandPreset.BAN, "ban {PLAYER} Cheating", true,
                Optional.of("dramatic_ban")).ladderText());
        for (String configured : defaults) {
            LadderCommand command = LadderCommand.parse(configured);
            if (!command.consoleCommand()) {
                continue;
            }
            GuardBench bench = new GuardBench();
            FlagContext flag = new FlagContext(GuardBench.steve().id(), "Steve", 2, 0.97, 21.5);
            String resolved = PenaltyText.resolve(command.body(), flag);
            assertTrue(bench.ladder(resolved).isPresent(), configured + " -> " + bench.refusals);
            bench.bedrockPrefix = Optional.of(".");
            FlagContext bedrock = new FlagContext(GuardBench.player(".Pocket_Guy").id(), ".Pocket_Guy", 2, 0.97, 21.5);
            assertTrue(bench.against(PenaltyText.resolve(command.body(), bedrock),
                    GuardBench.player(".Pocket_Guy")).isPresent(), configured + " -> " + bench.refusals);
        }
    }

    @Test
    void builtInAnimationsCarryNoCommandsOfTheirOwn() {
        for (String animation : ANIMATIONS) {
            Map<String, Object> script = BundledResources.yaml("animations/" + animation + ".yml");
            assertTrue(!script.isEmpty(), animation);
            assertEquals(List.of(), commandKeys(script).toList(), animation);
        }
    }

    private static Stream<String> commandKeys(Object node) {
        if (node instanceof Map<?, ?> map) {
            return map.entrySet().stream().flatMap(entry -> Stream.concat(
                    String.valueOf(entry.getKey()).toLowerCase().contains("command")
                            ? Stream.of(String.valueOf(entry.getKey())) : Stream.empty(),
                    commandKeys(entry.getValue())));
        }
        if (node instanceof List<?> list) {
            return list.stream().flatMap(BundledCommandsTest::commandKeys);
        }
        return Stream.empty();
    }
}
