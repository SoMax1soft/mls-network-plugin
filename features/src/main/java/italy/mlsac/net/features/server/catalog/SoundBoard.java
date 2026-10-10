// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.catalog;

import italy.mlsac.net.alerting.SoundCatalog;
import italy.mlsac.net.features.live.StaffSounds;
import italy.mlsac.net.features.server.OwningThread;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

public final class SoundBoard implements SoundCatalog, StaffSounds {

    private final NamedConstants<Sound> sounds = new NamedConstants<>("org.bukkit.Sound", Sound.class);
    private final OwningThread threads;

    public SoundBoard(OwningThread threads) {
        this.threads = threads;
    }

    public Optional<Sound> find(String name) {
        return sounds.find(name);
    }

    @Override
    public boolean exists(String soundName) {
        return find(soundName).isPresent();
    }

    @Override
    public void play(UUID player, String sound, float volume, float pitch) {
        find(sound).ifPresent(resolved -> threads.withPlayer(player,
                listener -> listener.playSound(listener.getLocation(), resolved, volume, pitch)));
    }

    public void playAt(Location place, String sound, float volume, float pitch) {
        World world = place.getWorld();
        if (world != null) {
            find(sound).ifPresent(resolved -> world.playSound(place, resolved, volume, pitch));
        }
    }
}
