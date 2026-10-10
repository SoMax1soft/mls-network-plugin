// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import italy.mlsac.net.command.SubcommandName;
import italy.mlsac.net.module.PluginModule;
import italy.mlsac.net.pipeline.engine.InputTapSource;
import italy.mlsac.net.player.LagExemptions;
import java.util.List;

interface Edition {

    InputTapSource inputTaps();

    LagExemptions lagExemptions();

    List<PluginModule> extraModules();

    String featureLine(String nametags);

    boolean offers(SubcommandName command);
}
