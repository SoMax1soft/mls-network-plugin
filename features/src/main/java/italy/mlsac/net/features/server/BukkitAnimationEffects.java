// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server;

import italy.mlsac.net.banimation.AnimationEffects;
import italy.mlsac.net.banimation.Emission;
import italy.mlsac.net.banimation.FreezeSpec;
import italy.mlsac.net.banimation.ParticleLook;
import italy.mlsac.net.banimation.Placement;
import italy.mlsac.net.banimation.Point;
import italy.mlsac.net.banimation.TitleCue;
import italy.mlsac.net.features.live.ConsoleCommands;
import italy.mlsac.net.features.server.animation.ParticleBurst;
import italy.mlsac.net.features.server.animation.TakenItems;
import italy.mlsac.net.features.server.animation.VictimFreeze;
import italy.mlsac.net.features.server.catalog.SoundBoard;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.text.LegacyColorizer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class BukkitAnimationEffects implements AnimationEffects {

    static final int PICKUP_DELAY_TICKS = 20;
    static final String DROP_SOUND = "ENTITY_ITEM_PICKUP";
    static final float DROP_VOLUME = 0.5f;
    static final float DROP_PITCH = 1.0f;

    private final PlayerDirectory players;
    private final OwningThread threads;
    private final ConsoleCommands console;
    private final SoundBoard sounds;
    private final ParticleBurst particles;
    private final VictimFreeze freezes;
    private final LegacyColorizer colors;
    private final TakenItems taken = new TakenItems();

    public BukkitAnimationEffects(PlayerDirectory players, OwningThread threads, ConsoleCommands console,
                                  SoundBoard sounds, ParticleBurst particles, VictimFreeze freezes,
                                  LegacyColorizer colors) {
        this.players = players;
        this.threads = threads;
        this.console = console;
        this.sounds = sounds;
        this.particles = particles;
        this.freezes = freezes;
        this.colors = colors;
    }

    @Override
    public boolean online(UUID player) {
        return players.byId(player).map(PlayerView::online).orElse(false);
    }

    @Override
    public Optional<Placement> location(UUID player) {
        return present(player).map(found -> {
            Location at = found.getLocation();
            return new Placement(new Point(at.getX(), at.getY(), at.getZ()), at.getYaw(), at.getPitch());
        });
    }

    @Override
    public void closeInventory(UUID player) {
        present(player).ifPresent(Player::closeInventory);
    }

    @Override
    public void resyncInventory(UUID player) {
        present(player).ifPresent(Player::updateInventory);
    }

    @Override
    public void resyncInventoryNextTick(UUID player) {
        threads.laterWithPlayer(player, TaskTiming.after(1L), Player::updateInventory);
    }

    @Override
    public void freeze(UUID player, FreezeSpec freeze) {
        present(player).ifPresent(found -> freezes.apply(found, freeze));
    }

    @Override
    public void unfreeze(UUID player) {
        threads.withPlayer(player, freezes::lift);
    }

    @Override
    public int stripInventory(UUID player) {
        return present(player).map(taken::strip).orElse(0);
    }

    @Override
    public void dropItem(UUID player, int index, Point at) {
        present(player).ifPresent(found -> taken.take(player, index).ifPresent(item -> {
            Location place = new Location(found.getWorld(), at.x(), at.y(), at.z());
            drop(place, item);
            sounds.playAt(place, DROP_SOUND, DROP_VOLUME, DROP_PITCH);
        }));
    }

    @Override
    public void dropRemainingItems(UUID player, int fromIndex, Point at) {
        taken.release(player).ifPresent(stash -> {
            World world = Bukkit.getWorld(stash.world());
            List<ItemStack> rest = stash.from(fromIndex);
            if (world == null || rest.isEmpty()) {
                return;
            }
            Location place = new Location(world, at.x(), at.y(), at.z());
            threads.atLocation(place, () -> rest.forEach(item -> drop(place, item)));
        });
    }

    @Override
    public void teleport(UUID player, Placement target) {
        present(player).ifPresent(found -> {
            Point point = target.point();
            Location destination = new Location(found.getWorld(), point.x(), point.y(), point.z(), target.yaw(),
                    target.pitch());
            if (threads.folia()) {
                found.teleportAsync(destination);
            } else {
                found.teleport(destination);
            }
        });
    }

    @Override
    public void sound(UUID player, Point at, String sound, float volume, float pitch) {
        placeBeside(player, at).ifPresent(place -> sounds.playAt(place, sound, volume, pitch));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void title(UUID player, TitleCue title) {
        present(player).ifPresent(found -> found.sendTitle(colors.colorize(title.title()),
                colors.colorize(title.subtitle()), title.fadeIn(), title.stay(), title.fadeOut()));
    }

    @Override
    public void lightning(UUID player, Point at) {
        placeBeside(player, at).ifPresent(place -> place.getWorld().strikeLightningEffect(place));
    }

    @Override
    public void particles(UUID player, ParticleLook look, List<Emission> emissions, Point spread, double speed) {
        if (emissions.isEmpty()) {
            return;
        }
        placeBeside(player, emissions.get(0).at()).ifPresent(place -> particles.emit(place.getWorld(), look, emissions, spread,
                speed));
    }

    @Override
    public void runCommand(String command) {
        threads.onGlobal(() -> console.dispatch(command));
    }

    private static Optional<Location> placeBeside(UUID player, Point at) {
        return present(player).map(found -> new Location(found.getWorld(), at.x(), at.y(), at.z()));
    }

    private static Optional<Player> present(UUID player) {
        return Optional.ofNullable(Bukkit.getPlayer(player));
    }

    private static void drop(Location place, ItemStack item) {
        Item dropped = place.getWorld().dropItemNaturally(place, item);
        dropped.setPickupDelay(PICKUP_DELAY_TICKS);
    }
}
