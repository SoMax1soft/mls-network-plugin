// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.features.FeaturesAssembly;
import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.pipeline.PipelineAssembly;
import italy.mlsac.net.testkit.TestCore;
import org.junit.jupiter.api.Test;

class BenchCommandGoneTest {

    @Test
    void theBenchSubcommandNoLongerExists() {
        assertTrue(SubcommandName.parse("bench").isEmpty());
        TestCore core = new TestCore();
        CoreServices services = core.services();
        PipelineAssembly pipeline = new PipelineAssembly(services);
        FeaturesAssembly features = new FeaturesAssembly(services, pipeline.ports());
        assertFalse(features.subcommands().stream().anyMatch(verb -> verb.name().id().equals("bench")));
    }
}
