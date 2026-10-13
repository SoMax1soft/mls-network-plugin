// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.guard;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.guard.CommandRoots;
import italy.mlsac.net.guard.RootOwner;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.FormattedCommandAlias;
import org.bukkit.command.PluginIdentifiableCommand;

public final class BukkitCommandRoots implements CommandRoots {

    private static final String VANILLA_WRAPPER = "VanillaCommandWrapper";

    private final PluginLog log;
    private volatile CommandMap map;

    public BukkitCommandRoots(PluginLog log) {
        this.log = log;
    }

    @Override
    public Optional<RootOwner> resolve(String label) {
        try {
            return commandMap().map(commands -> commands.getCommand(label)).map(BukkitCommandRoots::owner);
        } catch (RuntimeException failure) {
            log.debug(LogTopic.PENALTY, () -> "Command map lookup of '" + label + "' failed: " + failure);
            return Optional.empty();
        }
    }

    private Optional<CommandMap> commandMap() {
        CommandMap known = map;
        if (known != null) {
            return Optional.of(known);
        }
        Server server = Bukkit.getServer();
        if (server == null) {
            return Optional.empty();
        }
        try {
            Object found = server.getClass().getMethod("getCommandMap").invoke(server);
            if (found instanceof CommandMap commands) {
                map = commands;
                return Optional.of(commands);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException failure) {
            log.warn(LogTopic.PENALTY, "Command guard cannot read the server command map: " + failure);
        }
        return Optional.empty();
    }

    private static RootOwner owner(Command command) {
        if (command instanceof PluginIdentifiableCommand pluginCommand) {
            return new RootOwner(command.getName(), pluginCommand.getPlugin().getName());
        }
        if (command instanceof FormattedCommandAlias) {
            return new RootOwner(command.getName(), RootOwner.COMMANDS_FILE);
        }
        if (command.getClass().getSimpleName().contains(VANILLA_WRAPPER)) {
            return new RootOwner(command.getName(), RootOwner.MINECRAFT);
        }
        return new RootOwner(command.getName(), RootOwner.SERVER);
    }
}
