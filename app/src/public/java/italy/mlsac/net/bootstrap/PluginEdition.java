// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.module.PluginModule;
import italy.mlsac.net.pipeline.engine.InputTapSource;
import italy.mlsac.net.player.LagExemptions;
import java.util.List;
import java.util.Objects;

final class PluginEdition implements Edition {

    PluginEdition(CoreServices services) {
        Objects.requireNonNull(services, "services");
    }

    @Override
    public InputTapSource inputTaps() {
        return InputTapSource.NONE;
    }

    @Override
    public LagExemptions lagExemptions() {
        return LagExemptions.NONE;
    }

    @Override
    public List<PluginModule> extraModules() {
        return List.of();
    }

    @Override
    public String featureLine(String nametags) {
        return "Nametags: " + nametags;
    }

    @Override
    public boolean offers(SubcommandName command) {
        return true;
    }
}
