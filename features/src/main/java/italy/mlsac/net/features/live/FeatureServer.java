// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.alerting.SoundCatalog;
import italy.mlsac.net.banimation.AnimationEffects;
import italy.mlsac.net.banimation.EffectRegistry;
import italy.mlsac.net.guard.ConsoleGate;
import italy.mlsac.net.menu.MaterialCatalog;
import italy.mlsac.net.menu.MenuCanvas;
import italy.mlsac.net.nametag.NametagPorts;
import italy.mlsac.net.response.TrickHands;

public interface FeatureServer {

    SoundCatalog sounds();

    StaffSounds staffSounds();

    ConsoleCommands console();

    ConsoleGate consoleGate();

    EffectRegistry effectRegistry();

    AnimationEffects animationEffects();

    MaterialCatalog materials();

    TrickHands tricks();

    MenuCanvas menuCanvas(SuspectRows rows);

    NametagPorts nametagPorts(SuspectRows rows);

    void start(FeatureHooks hooks);

    void stop();
}
