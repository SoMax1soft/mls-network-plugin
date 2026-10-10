// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.lifecycle.PlayerTick;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.packet.HeldPacket;
import italy.mlsac.net.packet.Registration;
import italy.mlsac.net.player.PlayerChangedWorld;
import italy.mlsac.net.player.PlayerDied;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.player.PlayerRespawned;
import italy.mlsac.net.player.PlayerTeleported;
import italy.mlsac.net.suppression.SuppressionKit;
import italy.mlsac.net.suppression.lag.motion.SetbackTerrain;
import italy.mlsac.net.suppression.live.SuppressionServer;
import italy.mlsac.net.suppression.server.EffectWatcher;
import italy.mlsac.net.suppression.server.SuspectBodies;
import italy.mlsac.net.suppression.server.SuspectStatus;
import italy.mlsac.net.suppression.server.bukkit.body.BukkitSuspectBodies;
import italy.mlsac.net.suppression.server.bukkit.body.ItemOwnerLock;
import italy.mlsac.net.suppression.server.bukkit.cadence.LagCadence;
import italy.mlsac.net.suppression.server.bukkit.cadence.LagLifeHooks;
import italy.mlsac.net.suppression.server.bukkit.cadence.ObservedTicks;
import italy.mlsac.net.suppression.server.bukkit.cadence.OwnTeleports;
import italy.mlsac.net.suppression.server.bukkit.cadence.TickRoster;
import italy.mlsac.net.suppression.server.bukkit.damage.AttackerTrace;
import italy.mlsac.net.suppression.server.bukkit.damage.CausingPlayer;
import italy.mlsac.net.suppression.server.bukkit.damage.ComponentFault;
import italy.mlsac.net.suppression.server.bukkit.damage.DamageWatch;
import italy.mlsac.net.suppression.server.bukkit.delay.DaemonReleaseTimer;
import italy.mlsac.net.suppression.server.bukkit.delay.HeldRelease;
import italy.mlsac.net.suppression.server.bukkit.delay.HitDelayGate;
import italy.mlsac.net.suppression.server.bukkit.motion.BodyConditions;
import italy.mlsac.net.suppression.server.bukkit.motion.BukkitLagSubjects;
import italy.mlsac.net.suppression.server.bukkit.motion.BukkitSetbackTerrain;
import italy.mlsac.net.suppression.server.bukkit.motion.ClimbCheck;
import italy.mlsac.net.suppression.server.bukkit.motion.DirectTeleport;
import italy.mlsac.net.suppression.server.bukkit.motion.InPlaceTurn;
import italy.mlsac.net.suppression.server.bukkit.motion.PositionSyncTurn;
import italy.mlsac.net.suppression.server.bukkit.motion.RegionTeleport;
import italy.mlsac.net.suppression.server.bukkit.motion.TeleportRoute;
import italy.mlsac.net.suppression.server.bukkit.motion.ViewTurn;
import italy.mlsac.net.suppression.server.bukkit.motion.ViewTurnChoice;
import italy.mlsac.net.suppression.server.bukkit.presence.GameModePauses;
import italy.mlsac.net.suppression.server.bukkit.presence.GameModeWatch;
import italy.mlsac.net.suppression.server.bukkit.presence.SuspectPresence;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class BukkitSuppressionServer implements SuppressionServer {

    private final SuppressionHostParts parts;
    private final GameModePauses modePauses = new GameModePauses();
    private final SuspectPresence presence;
    private final BukkitSuspectBodies bodies;
    private final TickRoster roster;
    private final BukkitSetbackTerrain terrain = new BukkitSetbackTerrain();
    private final ShutdownSteps shutdown = new ShutdownSteps();

    public BukkitSuppressionServer(SuppressionHostParts parts) {
        this.parts = parts;
        this.presence = new SuspectPresence(parts.players(), parts.regions(), modePauses);
        this.bodies = new BukkitSuspectBodies(ItemOwnerLock.forThisServer());
        this.roster = new TickRoster(parts.clock());
    }

    @Override
    public SuspectStatus status() {
        return presence;
    }

    @Override
    public SuspectBodies bodies() {
        return bodies;
    }

    @Override
    public EffectWatcher watcher() {
        return roster;
    }

    @Override
    public SetbackTerrain terrain() {
        return terrain;
    }

    @Override
    public void start(SuppressionKit<HeldPacket> kit) {
        ObservedTicks ticks = new ObservedTicks();
        OwnTeleports ownTeleports = new OwnTeleports();
        listen(ServerTick.class, ticks::serverTicked);
        listen(PlayerTick.class, ticks::playerTicked);
        listen(PlayerLeft.class, ticks::playerLeft);
        LagLifeHooks hooks = new LagLifeHooks(kit.lagRuntimes(), ticks, ownTeleports);
        listen(PlayerDied.class, hooks::died);
        listen(PlayerRespawned.class, hooks::respawned);
        listen(PlayerChangedWorld.class, hooks::worldSwitched);
        listen(PlayerTeleported.class, hooks::teleported);
        startHitDelay(kit);
        parts.host().plugin().ifPresent(plugin -> startOnServer(plugin, kit, ticks, ownTeleports));
    }

    @Override
    public void stop() {
        shutdown.runAll();
        roster.clear();
        modePauses.clear();
    }

    private void startHitDelay(SuppressionKit<HeldPacket> kit) {
        DaemonReleaseTimer timer = new DaemonReleaseTimer(parts.log());
        shutdown.add(timer::close);
        HeldRelease release = new HeldRelease(kit.delay(), kit.lagRuntimes(), parts.packets().sender(), parts.clock(),
                timer);
        HitDelayGate gate = new HitDelayGate(parts.packets(), kit.delay(), kit.readout(), kit.lagRuntimes(),
                kit.combat(), parts.clock(), release);
        for (Registration registration : gate.register()) {
            shutdown.add(registration::unregister);
        }
    }

    private void startOnServer(Plugin plugin, SuppressionKit<HeldPacket> kit, ObservedTicks ticks,
                               OwnTeleports ownTeleports) {
        GameModeWatch modes = new GameModeWatch(modePauses);
        modes.seed(Bukkit.getOnlinePlayers());
        register(plugin, modes);
        register(plugin, new DamageWatch(kit.damage(), new AttackerTrace(CausingPlayer.forThisServer()),
                new ComponentFault(parts.log()), kit.lagRuntimes(), kit.combat(), ticks, parts.clock()));
        TeleportRoute route = parts.platform().folia()
                ? new RegionTeleport(ownTeleports, parts.scheduler(), parts.log())
                : new DirectTeleport(ownTeleports);
        ViewTurn viewTurn = ViewTurnChoice.standard().choose(parts.packets().serverVersion(),
                new PositionSyncTurn(parts.packets().sender()), new InPlaceTurn(route), parts.log());
        LagCadence cadence = new LagCadence(roster, kit.ticker(),
                new BukkitLagSubjects(new BodyConditions(ClimbCheck.forThisServer()), route, viewTurn), parts.clock());
        listen(ServerTick.class, cadence::serverTicked);
        listen(PlayerTick.class, cadence::playerTicked);
    }

    private <E> void listen(Class<E> type, Consumer<? super E> handler) {
        EventBus bus = parts.bus();
        Subscription subscription = bus.subscribe(type, handler);
        shutdown.add(subscription::cancel);
    }

    private void register(Plugin plugin, Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        shutdown.add(() -> HandlerList.unregisterAll(listener));
    }
}
