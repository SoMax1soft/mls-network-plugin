// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server;

import italy.mlsac.net.alerting.SoundCatalog;
import italy.mlsac.net.banimation.AnimationEffects;
import italy.mlsac.net.banimation.EffectRegistry;
import italy.mlsac.net.features.live.ConsoleCommands;
import italy.mlsac.net.features.live.FeatureHooks;
import italy.mlsac.net.features.live.FeatureServer;
import italy.mlsac.net.features.live.GuardAlarms;
import italy.mlsac.net.features.live.GuardMessages;
import italy.mlsac.net.features.live.GuardedConsole;
import italy.mlsac.net.features.live.PluginLogbook;
import italy.mlsac.net.features.live.StaffSounds;
import italy.mlsac.net.features.live.SuspectRows;
import italy.mlsac.net.features.server.animation.AnimationLockdown;
import italy.mlsac.net.features.server.animation.IncomingActionShield;
import italy.mlsac.net.features.server.animation.InventoryResync;
import italy.mlsac.net.features.server.animation.LockdownTable;
import italy.mlsac.net.features.server.animation.ParticleBurst;
import italy.mlsac.net.features.server.animation.VictimFreeze;
import italy.mlsac.net.features.server.catalog.BukkitEffectRegistry;
import italy.mlsac.net.features.server.catalog.BukkitMaterials;
import italy.mlsac.net.features.server.catalog.ConsoleDispatch;
import italy.mlsac.net.features.server.catalog.PotionKinds;
import italy.mlsac.net.features.server.catalog.SoundBoard;
import italy.mlsac.net.features.server.guard.BukkitCommandRoots;
import italy.mlsac.net.features.server.guard.FloodgatePrefix;
import italy.mlsac.net.features.server.menu.ItemFaces;
import italy.mlsac.net.features.server.menu.MenuClickRouting;
import italy.mlsac.net.features.server.menu.OpenMenus;
import italy.mlsac.net.features.server.nametag.NametagLifecycle;
import italy.mlsac.net.features.server.nametag.NametagPacketFollower;
import italy.mlsac.net.features.server.nametag.NametagWire;
import italy.mlsac.net.features.server.nametag.SpigotTrackingRange;
import italy.mlsac.net.features.server.response.BukkitTricks;
import italy.mlsac.net.features.server.response.ReducedDamageWatch;
import italy.mlsac.net.features.server.response.ShuffleOrder;
import italy.mlsac.net.guard.CommandGuard;
import italy.mlsac.net.guard.CommandOrigin;
import italy.mlsac.net.guard.ConsoleGate;
import italy.mlsac.net.guard.DispatchBudget;
import italy.mlsac.net.guard.PlayerNames;
import italy.mlsac.net.guard.RootPolicy;
import italy.mlsac.net.guard.TargetRule;
import italy.mlsac.net.menu.MaterialCatalog;
import italy.mlsac.net.menu.MenuCanvas;
import italy.mlsac.net.module.CoreServices;
import italy.mlsac.net.nametag.NametagPorts;
import italy.mlsac.net.platform.RegistryNameAliases;
import italy.mlsac.net.player.Audience;
import italy.mlsac.net.response.TrickHands;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import italy.mlsac.net.text.HexColorNormalizer;
import italy.mlsac.net.text.LegacyColorizer;
import java.util.List;
import java.util.Optional;
import org.bukkit.plugin.Plugin;

public final class BukkitFeatureServer implements FeatureServer {

    static final String DAMAGE_CLOCK = "responses.clock";
    static final long ADOPTION_PERIOD_TICKS = 10L;
    static final String GUARD_CLOCK = "guard.clock";

    private final CoreServices core;
    private final OwningThread threads;
    private final SoundBoard sounds;
    private final ConsoleDispatch console;
    private final CommandGuard guard;
    private final BukkitEffectRegistry effects;
    private final BukkitMaterials materials = new BukkitMaterials();
    private final BukkitTricks tricks;
    private final LegacyColorizer colors = new LegacyColorizer(new HexColorNormalizer());
    private final NametagWire wire;
    private final OpenMenus openMenus = new OpenMenus();
    private final EventSubscriptions subscriptions = new EventSubscriptions();
    private final AnimationEffects animationEffects;
    private NametagPorts nametagPorts;
    private NametagPacketFollower follower;
    private IncomingActionShield actionShield;
    private Optional<TaskTicket> adoption = Optional.empty();

    public BukkitFeatureServer(CoreServices core) {
        this.core = core;
        this.threads = new OwningThread(core.scheduler(), core.platform().folia(), core.log());
        this.sounds = new SoundBoard(threads);
        this.console = new ConsoleDispatch(core.log());
        this.guard = commandGuard(core);
        this.effects = new BukkitEffectRegistry(sounds, core.serverRules());
        this.tricks = new BukkitTricks(new ShuffleOrder(core.random()));
        this.wire = new NametagWire(core.packets().sender(), colors, core.workload());
        this.animationEffects = new BukkitAnimationEffects(core.players(), threads, console, sounds,
                new ParticleBurst(effects, core.log()),
                new VictimFreeze(new PotionKinds(new RegistryNameAliases(core.serverRules()))), colors);
    }

    @Override
    public SoundCatalog sounds() {
        return sounds;
    }

    @Override
    public StaffSounds staffSounds() {
        return sounds;
    }

    @Override
    public ConsoleCommands console() {
        return console;
    }

    @Override
    public ConsoleGate consoleGate() {
        return guard;
    }

    @Override
    public EffectRegistry effectRegistry() {
        return effects;
    }

    @Override
    public AnimationEffects animationEffects() {
        return animationEffects;
    }

    @Override
    public MaterialCatalog materials() {
        return materials;
    }

    @Override
    public TrickHands tricks() {
        return tricks;
    }

    @Override
    public MenuCanvas menuCanvas(SuspectRows rows) {
        return new BukkitMenuCanvas(core.players(), core.scheduler(), threads,
                new GuardedConsole(console, guard, CommandOrigin.menuButton()), rows, new ItemFaces(materials),
                openMenus);
    }

    @Override
    public synchronized NametagPorts nametagPorts(SuspectRows rows) {
        nametagPorts = new BukkitNametagPorts(rows, core.players(), core.packets().sender(), wire, new SpigotTrackingRange());
        return nametagPorts;
    }

    @Override
    public synchronized void start(FeatureHooks hooks) {
        actionShield = new IncomingActionShield(hooks.animations());
        actionShield.listen(core.packets());
        if (!core.platform().folia()) {
            follower = new NametagPacketFollower(hooks.nametags(), wire, core.workload());
            follower.listen(core.packets());
        }
        core.host().plugin().ifPresent(plugin -> hookServer(plugin, hooks));
    }

    @Override
    public synchronized void stop() {
        adoption.ifPresent(TaskTicket::cancel);
        adoption = Optional.empty();
        if (actionShield != null) {
            actionShield.deafen();
            actionShield = null;
        }
        if (follower != null) {
            follower.deafen();
            follower = null;
        }
        subscriptions.unregisterAll();
    }

    private static CommandGuard commandGuard(CoreServices core) {
        return new CommandGuard(() -> core.config().get().core().commandGuard(),
                new RootPolicy(new BukkitCommandRoots(core.log())),
                new TargetRule(new PlayerNames(new FloodgatePrefix(core.log()))), new DispatchBudget(),
                new GuardAlarms(core.messages(), GuardMessages.defaults(),
                        line -> core.notices().broadcast(Audience.STAFF, List.of(line)), new PluginLogbook(core.log()),
                        () -> core.clock().wallMillis(GUARD_CLOCK)),
                () -> core.clock().wallMillis(GUARD_CLOCK));
    }

    private void hookServer(Plugin plugin, FeatureHooks hooks) {
        subscriptions.register(plugin, new MenuClickRouting(hooks.menus(), openMenus));
        subscriptions.attach(plugin, new AnimationLockdown(hooks.animations(),
                new LockdownTable(new InventoryResync(threads)).actions()));
        subscriptions.register(plugin, new ReducedDamageWatch(hooks.reductions(),
                () -> core.clock().wallMillis(DAMAGE_CLOCK)));
        if (nametagPorts != null && !core.platform().folia()) {
            NametagLifecycle lifecycle = new NametagLifecycle(hooks.nametags(), core.packets().clients(),
                    nametagPorts);
            subscriptions.register(plugin, lifecycle);
            lifecycle.adoptCurrent();
            adoption = Optional.of(core.scheduler().submit(TaskLane.MAIN,
                    TaskTiming.repeating(ADOPTION_PERIOD_TICKS, ADOPTION_PERIOD_TICKS), lifecycle::adoptCurrent));
        }
    }
}
