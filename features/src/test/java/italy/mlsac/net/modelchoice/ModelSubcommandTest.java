// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.modelchoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.api.ModelChoice;
import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.config.ConfigReference;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.features.live.DirectoryLookup;
import italy.mlsac.net.testkit.FakeActor;
import italy.mlsac.net.testkit.TestConfigs;
import italy.mlsac.net.testkit.TestCore;
import italy.mlsac.net.text.MessageBook;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModelSubcommandTest {

    private final TestCore core = new TestCore();
    private final UUID aura = core.players.add("Aura_00").id();
    private final ModelSubcommand command;
    private final FakeActor admin = FakeActor.console();
    private final MessageBook english = TestConfigs.messages(new ConfigReference(
            EffectiveConfig.pluginDefaults(TestConfigs.core("language: en\n"))));

    ModelSubcommandTest() {
        english.registerDefaults(ModelChoiceTexts.defaults());
        command = new ModelSubcommand(new DirectoryLookup(core.players), core.selections, english);
    }

    @Test
    void theVerbIsModelWithItsOwnPermission() {
        assertEquals(SubcommandName.MODEL, command.name());
        assertTrue(SubcommandName.MODEL.grants().stream().anyMatch(grant -> grant.node().equals("mlsac.command.model")));
    }

    @Test
    void setsShowsAndClearsTheChoice() {
        command.execute(admin, List.of("Aura_00", "pro"));
        assertEquals(ModelChoice.PRO, core.selections.get(aura).effective());
        assertTrue(last().contains("Aura_00") && last().contains("is now checked by") && last().contains("Pro"), last());

        command.execute(admin, List.of("Aura_00"));
        assertTrue(last().contains("Available:") && last().contains("Ultra"), last());

        command.execute(admin, List.of("Aura_00", "DEFAULT"));
        assertEquals(ModelChoice.DEFAULT, core.selections.get(aura).requested());
        assertTrue(last().contains("server default"), last());
    }

    @Test
    void aFallbackTellsTheReason() {
        core.apply(TestConfigs.withModels(core.config.get(), true, "fast"));
        command.execute(admin, List.of("Aura_00", "pro"));
        assertTrue(last().contains("not available") && last().contains("switched off in the preset"), last());
        assertEquals(ModelChoice.DEFAULT, core.selections.get(aura).effective());
    }

    @Test
    void wrongInputIsExplained() {
        command.execute(admin, List.of());
        assertTrue(last().contains("/mlsac model"), last());
        command.execute(admin, List.of("Nobody"));
        assertTrue(last().contains("Nobody"), last());
        command.execute(admin, List.of("Aura_00", "ultra80"));
        assertTrue(last().contains("Unknown model") && last().contains("ultra80"), last());
        assertEquals(ModelChoice.DEFAULT, core.selections.get(aura).requested());
    }

    @Test
    void completesPlayersAndChoices() {
        assertEquals(List.of("Aura_00"), command.complete(admin, List.of("au")));
        assertEquals(List.of("ultra"), command.complete(admin, List.of("Aura_00", "u")));
        assertEquals(List.of("default", "pro", "mini", "ultra"), command.complete(admin, List.of("Aura_00", "")));
    }

    private String last() {
        List<String> lines = admin.received();
        return lines.get(lines.size() - 1);
    }
}
