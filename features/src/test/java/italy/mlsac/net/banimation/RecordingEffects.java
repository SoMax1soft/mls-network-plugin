// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class RecordingEffects implements AnimationEffects {

    final Map<UUID, Placement> online = new HashMap<>();
    final List<String> calls = new ArrayList<>();
    final List<String> commands = new ArrayList<>();
    final List<Placement> teleports = new ArrayList<>();
    final List<Integer> droppedIndexes = new ArrayList<>();
    final List<List<Emission>> particleBursts = new ArrayList<>();
    int itemsInInventory = 36;
    RuntimeException failOnSound;

    @Override
    public boolean online(UUID player) {
        return online.containsKey(player);
    }

    @Override
    public Optional<Placement> location(UUID player) {
        return Optional.ofNullable(online.get(player));
    }

    @Override
    public void closeInventory(UUID player) {
        calls.add("close");
    }

    @Override
    public void resyncInventory(UUID player) {
        calls.add("resync");
    }

    @Override
    public void resyncInventoryNextTick(UUID player) {
        calls.add("resync-later");
    }

    @Override
    public void freeze(UUID player, FreezeSpec freeze) {
        calls.add("freeze " + freeze);
    }

    @Override
    public void unfreeze(UUID player) {
        calls.add("unfreeze");
    }

    @Override
    public int stripInventory(UUID player) {
        calls.add("strip");
        return itemsInInventory;
    }

    @Override
    public void dropItem(UUID player, int index, Point at) {
        droppedIndexes.add(index);
    }

    @Override
    public void dropRemainingItems(UUID player, int fromIndex, Point at) {
        calls.add("drop-rest " + fromIndex + " at " + at);
    }

    @Override
    public void teleport(UUID player, Placement target) {
        teleports.add(target);
    }

    @Override
    public void sound(UUID player, Point at, String sound, float volume, float pitch) {
        if (failOnSound != null) {
            throw failOnSound;
        }
        calls.add("sound " + sound);
    }

    @Override
    public void title(UUID player, TitleCue title) {
        calls.add("title " + title.title());
    }

    @Override
    public void lightning(UUID player, Point at) {
        calls.add("lightning");
    }

    @Override
    public void particles(UUID player, ParticleLook look, List<Emission> emissions, Point spread, double speed) {
        particleBursts.add(emissions);
    }

    @Override
    public void runCommand(String command) {
        commands.add(command);
    }
}
