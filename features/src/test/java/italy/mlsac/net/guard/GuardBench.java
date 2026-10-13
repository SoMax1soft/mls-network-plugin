// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import italy.mlsac.net.config.guard.CommandGuardSettings;
import italy.mlsac.net.config.guard.GuardMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GuardBench implements CommandRoots, BedrockPrefix, GuardReport {

    public final Map<String, RootOwner> commands = new HashMap<>();
    public final List<String> refusals = new ArrayList<>();
    public final List<String> bursts = new ArrayList<>();
    public Optional<String> bedrockPrefix = Optional.empty();
    public CommandGuardSettings settings = CommandGuardSettings.pluginDefaults();
    public long now = 1_789_000_000_000L;
    public final CommandGuard guard;

    public GuardBench() {
        vanilla("ban", "ban-ip", "kick", "pardon", "pardon-ip", "op", "deop", "stop", "reload", "whitelist",
                "execute", "gamemode", "give", "tp", "say", "save-off", "fill");
        server("plugins", "pl", "version");
        plugin("Essentials", "broadcast", "ban", "tempban", "mute", "kick", "togglejail", "sudo");
        alias("bc", "broadcast", "Essentials");
        alias("eban", "ban", "Essentials");
        alias("jail", "togglejail", "Essentials");
        alias("essentials:ban", "ban", "Essentials");
        plugin("LiteBans", "tempban", "mute", "warn", "litebans");
        alias("litebans:tempban", "tempban", "LiteBans");
        plugin("CMI", "cmi");
        plugin("MLSAC", "mlsac");
        alias("mls", "mlsac", "MLSAC");
        alias("ml", "mlsac", "MLSAC");
        plugin("LuckPerms", "lp", "luckperms");
        plugin("PlugManX", "plugman");
        plugin("WorldEdit", "/set", "brush");
        plugin("Skript", "skript", "effect");
        plugin("NiceTools", "heal");
        alias("promote", "op", "SneakyRanks");
        this.guard = new CommandGuard(() -> settings, new RootPolicy(this), new TargetRule(new PlayerNames(this)),
                new DispatchBudget(), this, () -> now);
    }

    public static PunishedPlayer steve() {
        return new PunishedPlayer("Steve", UUID.nameUUIDFromBytes("Steve".getBytes()));
    }

    public static PunishedPlayer player(String name) {
        return new PunishedPlayer(name, UUID.nameUUIDFromBytes(name.getBytes()));
    }

    public Optional<String> ladder(String command) {
        return guard.admit(ConsoleOrder.against(command, CommandOrigin.ladderStep(2), steve()));
    }

    public Optional<String> against(String command, PunishedPlayer target) {
        return guard.admit(ConsoleOrder.against(command, CommandOrigin.ladderStep(2), target));
    }

    public Optional<String> menu(String command) {
        return guard.admit(ConsoleOrder.untargeted(command, CommandOrigin.menuButton()));
    }

    public String lastReason() {
        return refusals.isEmpty() ? "" : refusals.get(refusals.size() - 1);
    }

    public void limits(GuardMode mode, Set<String> extra, int perMinute, int perPlayer, int burst, int pause) {
        settings = new CommandGuardSettings(true, mode, extra, perMinute, perPlayer, burst, pause);
    }

    public void extra(String... roots) {
        limits(GuardMode.BLOCK, Set.of(roots), CommandGuardSettings.DEFAULT_MAX_PER_MINUTE,
                CommandGuardSettings.DEFAULT_MAX_PER_PLAYER_PER_MINUTE,
                CommandGuardSettings.DEFAULT_MAX_SAME_COMMAND_BURST, CommandGuardSettings.DEFAULT_BURST_PAUSE_MINUTES);
    }

    private void vanilla(String... names) {
        for (String name : names) {
            commands.put(name, new RootOwner(name, RootOwner.MINECRAFT));
            commands.put("minecraft:" + name, new RootOwner(name, RootOwner.MINECRAFT));
        }
    }

    private void server(String... names) {
        for (String name : names) {
            commands.put(name, new RootOwner(name, RootOwner.SERVER));
            commands.put("bukkit:" + name, new RootOwner(name, RootOwner.SERVER));
        }
    }

    private void plugin(String plugin, String... names) {
        for (String name : names) {
            commands.put(name, new RootOwner(name, plugin));
            commands.put(plugin.toLowerCase() + ":" + name, new RootOwner(name, plugin));
        }
    }

    private void alias(String label, String command, String plugin) {
        commands.put(label, new RootOwner(command, plugin));
    }

    @Override
    public Optional<RootOwner> resolve(String label) {
        return Optional.ofNullable(commands.get(label));
    }

    @Override
    public Optional<String> prefix() {
        return bedrockPrefix;
    }

    @Override
    public void refused(ConsoleOrder order, GuardRefusal refusal, GuardMode mode) {
        refusals.add(refusal.reason().name());
    }

    @Override
    public void burst(String rootKey, int players, int pauseMinutes, GuardMode mode) {
        bursts.add(rootKey + " " + players + " " + pauseMinutes);
    }
}
