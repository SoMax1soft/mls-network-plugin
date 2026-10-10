// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnimationEffects {

    boolean online(UUID player);

    Optional<Placement> location(UUID player);

    void closeInventory(UUID player);

    void resyncInventory(UUID player);

    void resyncInventoryNextTick(UUID player);

    void freeze(UUID player, FreezeSpec freeze);

    void unfreeze(UUID player);

    int stripInventory(UUID player);

    void dropItem(UUID player, int index, Point at);

    void dropRemainingItems(UUID player, int fromIndex, Point at);

    void teleport(UUID player, Placement target);

    void sound(UUID player, Point at, String sound, float volume, float pitch);

    void title(UUID player, TitleCue title);

    void lightning(UUID player, Point at);

    void particles(UUID player, ParticleLook look, List<Emission> emissions, Point spread, double speed);

    void runCommand(String command);
}
