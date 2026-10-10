// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.module.PluginModule;
import italy.mlsac.net.pipeline.PipelineAssembly;
import italy.mlsac.net.pipeline.engine.InputTapSource;
import italy.mlsac.net.player.LagExemptions;
import italy.mlsac.net.testkit.TestCore;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EditionTest {

    private final CoreServices services = new TestCore().services();
    private final Edition edition = new PluginEdition(services);

    @Test
    void publicEditionHasNoRecorder() {
        if (!"public".equals(System.getProperty("mlsac.edition"))) {
            return;
        }
        assertSame(InputTapSource.NONE, edition.inputTaps());
        assertSame(LagExemptions.NONE, edition.lagExemptions());
        assertTrue(edition.extraModules().isEmpty());
        String line = edition.featureLine("on").toLowerCase(Locale.ROOT);
        assertFalse(line.contains("collection") || line.contains("data"), line);
        assertFalse(classExists("italy.mlsac.net.recording.RecordingAssembly"));
        assertFalse(classExists("italy.mlsac.net.recording.dataset.RecordingDesk"));
    }

    @Test
    void internalEditionAddsTheRecorderWithTheOldSummary() {
        if (!"internal".equals(System.getProperty("mlsac.edition"))) {
            return;
        }
        List<PluginModule> extra = edition.extraModules();
        assertEquals(List.of("recording"), extra.stream().map(PluginModule::name).toList());
        assertEquals(3, extra.get(0).subcommands().size());
        new PipelineAssembly(services, edition.inputTaps());
        assertFalse(edition.lagExemptions().exempt(UUID.randomUUID()));
        assertEquals("Data collection: output data | nametags on", edition.featureLine("on"));
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, EditionTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException absent) {
            return false;
        }
    }
}
