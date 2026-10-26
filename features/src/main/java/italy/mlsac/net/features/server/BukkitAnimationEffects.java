// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server;

import italy.mlsac.net.banimation.ActorGesture;
import italy.mlsac.net.banimation.ActorKind;
import italy.mlsac.net.banimation.ActorPose;
import italy.mlsac.net.banimation.AnimationEffects;
import italy.mlsac.net.banimation.Emission;
import italy.mlsac.net.banimation.FreezeSpec;
import italy.mlsac.net.banimation.ParticleLook;
import italy.mlsac.net.banimation.Placement;
import italy.mlsac.net.banimation.Point;
import italy.mlsac.net.banimation.TitleCue;
import italy.mlsac.net.banimation.WorldSpot;
import italy.mlsac.net.features.live.ConsoleCommands;
import italy.mlsac.net.features.server.animation.FakeActors;
import italy.mlsac.net.features.server.animation.ParticleBurst;
import italy.mlsac.net.features.server.animation.TakenItems;
import italy.mlsac.net.features.server.animation.VictimFreeze;
import italy.mlsac.net.features.server.catalog.SoundBoard;
import italy.mlsac.net.packet.ClientVersions;
import italy.mlsac.net.platform.GameVersion;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.text.LegacyColorizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    static final double ACTOR_AUDIENCE_RADIUS = 64.0;
    static final GameVersion ACTOR_CLIENTS_FROM = GameVersion.of(1, 19, 0);
    static final int GROUND_SEARCH_UP = 2;
    static final int GROUND_SEARCH_DOWN = 6;

    private final PlayerDirectory players;
    private final OwningThread threads;
    private final ConsoleCommands console;
    private final SoundBoard sounds;
    private final ParticleBurst particles;
    private final VictimFreeze freezes;
    private final LegacyColorizer colors;
    private final FakeActors actors;
    private final ClientVersions clients;
    private final TakenItems taken = new TakenItems();
    private final Map<UUID, UUID> detachedWorlds = new ConcurrentHashMap<>();

    public BukkitAnimationEffects(PlayerDirectory players, OwningThread threads, ConsoleCommands console,
                                  SoundBoard sounds, ParticleBurst particles, VictimFreeze freezes,
                                  LegacyColorizer colors, FakeActors actors, ClientVersions clients) {
        this.players = players;
        this.threads = threads;
        this.console = console;
        this.sounds = sounds;
        this.particles = particles;
        this.freezes = freezes;
        this.colors = colors;
        this.actors = actors;
        this.clients = clients;
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
    public Optional<WorldSpot> spot(UUID player) {
        return present(player).map(found -> {
            Location at = found.getLocation();
            UUID world = found.getWorld().getUID();
            detachedWorlds.put(player, world);
            return new WorldSpot(world, new Point(at.getX(), at.getY(), at.getZ()));
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
    public void spawnActor(UUID player, ActorKind kind, Placement at, ActorPose pose) {
        present(player).ifPresent(found -> {
            World world = found.getWorld();
            Point point = at.point();
            Location place = new Location(world, point.x(), point.y(), point.z());
            place.setY(groundUnder(place));
            actors.spawn(player, kind, new Placement(new Point(place.getX(), place.getY(), place.getZ()), at.yaw(),
                    at.pitch()), pose, audience(world, place));
        });
    }

    @Override
    public void poseActor(UUID player, ActorPose pose) {
        actors.pose(player, pose);
    }

    @Override
    public void moveActor(UUID player, Placement to) {
        actors.move(player, to);
    }

    @Override
    public void gestureActor(UUID player, ActorGesture gesture) {
        actors.gesture(player, gesture);
    }

    @Override
    public void removeActor(UUID player) {
        actors.remove(player);
        detachedWorlds.remove(player);
    }

    @Override
    public void hideVictim(UUID player, int ticks) {
        present(player).ifPresent(found -> freezes.hide(found, ticks));
    }

    @Override
    public void runCommand(String command) {
        threads.onGlobal(() -> console.dispatch(command));
    }

    private List<UUID> audience(World world, Location place) {
        double reach = ACTOR_AUDIENCE_RADIUS * ACTOR_AUDIENCE_RADIUS;
        List<UUID> viewers = new ArrayList<>();
        for (Player watcher : world.getPlayers()) {
            UUID id = watcher.getUniqueId();
            if (watcher.getLocation().distanceSquared(place) <= reach
                    && clients.version(id).map(version -> version.atLeast(ACTOR_CLIENTS_FROM)).orElse(true)) {
                viewers.add(id);
            }
        }
        return viewers;
    }

    private double groundUnder(Location place) {
        World world = place.getWorld();
        int x = place.getBlockX();
        int z = place.getBlockZ();
        int feet = place.getBlockY();
        try {
            if (threads.folia() && !Bukkit.isOwnedByCurrentRegion(place)) {
                return place.getY();
            }
            for (int y = feet + GROUND_SEARCH_UP; y >= feet - GROUND_SEARCH_DOWN; y--) {
                if (world.getBlockAt(x, y - 1, z).getType().isSolid() && !world.getBlockAt(x, y, z).getType().isSolid()
                        && !world.getBlockAt(x, y + 1, z).getType().isSolid()) {
                    return y;
                }
            }
        } catch (RuntimeException unreadable) {
            return place.getY();
        }
        return place.getY();
    }

    private Optional<Location> placeBeside(UUID player, Point at) {
        Optional<World> world = Optional.ofNullable(detachedWorlds.get(player)).map(Bukkit::getWorld)
                .or(() -> present(player).map(Player::getWorld));
        return world.map(found -> new Location(found, at.x(), at.y(), at.z()));
    }

    private static Optional<Player> present(UUID player) {
        return Optional.ofNullable(Bukkit.getPlayer(player));
    }

    private static void drop(Location place, ItemStack item) {
        Item dropped = place.getWorld().dropItemNaturally(place, item);
        dropped.setPickupDelay(PICKUP_DELAY_TICKS);
    }
}
