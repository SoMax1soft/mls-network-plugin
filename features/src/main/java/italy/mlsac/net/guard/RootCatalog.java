// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RootCatalog {

    public static final String OWN_PLUGIN = "mlsac";
    public static final String OWN_COMMAND = "mlsac";
    public static final String OWN_PUNISH = "punish";

    static final Set<String> DENIED_ROOTS = Set.of(
            "op", "deop", "stop", "restart", "reload", "rl", "shutdown", "end", "greload", "gend",
            "plugman", "plugmanx", "pm", "plugins", "pl", "serverutils", "su",
            "lp", "luckperms", "lpb", "lpv", "perm", "perms", "permission", "permissions", "pex", "permissionsex",
            "manuadd", "manuaddp", "mangaddp", "manudel", "mangadd", "mangdel", "groupmanager", "upc",
            "ultrapermissions",
            "whitelist", "wl", "execute", "sudo", "esudo", "console", "function", "datapack",
            "eval", "script", "skript", "sk", "js", "javascript", "bsh", "groovy", "ex", "exs", "denizen",
            "we", "worldedit", "fawe", "fastasyncworldedit", "asyncworldedit", "fill", "setblock", "clone",
            "rg", "region", "worldguard", "wg",
            "gamemode", "gm", "gmc", "gms", "gma", "gmsp", "egamemode", "give", "i", "item", "egive",
            "mv", "multiverse", "mvdelete", "mvremove", "mvregen",
            "save-off", "save-on", "save-all", "tp", "teleport", "tphere", "tpall", "tpo", "etp", "etphere",
            "transfer", "publish", "rcon", "sendcommand", "consolecommand", "runcommand", "servercommand",
            "bungeecmd", "proxycmd");

    static final Set<String> DENIED_OWNERS = Set.of(
            "worldedit", "fastasyncworldedit", "asyncworldedit", "worldguard", "luckperms", "permissionsex",
            "groupmanager", "ultrapermissions", "powerranks", "plugman", "plugmanx", "serverutils",
            "skript", "denizen", "mycommand", "scriptblockplus", "conditionalevents", "multiverse-core");

    static final Set<String> VANILLA_PUNISHMENTS = Set.of("ban", "ban-ip", "kick", "pardon", "pardon-ip");

    static final Map<String, Set<String>> PLUGIN_PUNISHMENTS = Map.of(
            "litebans", Set.of("ban", "tempban", "ipban", "tempipban", "banip", "mute", "tempmute", "ipmute",
                    "tempipmute", "kick", "warn", "unban", "unmute", "unwarn"),
            "advancedban", Set.of("ban", "tempban", "ipban", "tempipban", "banip", "mute", "tempmute", "kick",
                    "warn", "tempwarn", "unban", "unmute", "unwarn"),
            "banmanager", Set.of("ban", "tempban", "banip", "tempbanip", "mute", "tempmute", "kick", "warn",
                    "tempwarn", "unban", "unmute"),
            "essentials", Set.of("ban", "tempban", "banip", "tempbanip", "mute", "kick", "jail", "togglejail",
                    "unban", "unbanip"),
            "cmi", Set.of("ban", "tempban", "banip", "tempbanip", "ipban", "kick", "mute", "tempmute", "jail",
                    "warn", "unban", "unmute"),
            "libertybans", Set.of("ban", "ipban", "mute", "ipmute", "warn", "kick", "unban", "unmute", "unwarn"),
            "maxbans", Set.of("ban", "tempban", "ipban", "tempipban", "mute", "tempmute", "kick", "warn", "unban",
                    "unmute"));

    static final Map<String, String> SUBCOMMAND_ROOTS = Map.of("cmi", "cmi", "libertybans", "libertybans");

    static final Set<String> FEEDBACK = Set.of("broadcast");

    static final Set<String> OWN_DENIED_SUBCOMMANDS = Set.of("reload", "reinstall");

    static final Set<String> PUNISHMENT_VERBS = punishmentVerbs();

    private RootCatalog() {
    }

    static boolean denied(String root) {
        return DENIED_ROOTS.contains(root);
    }

    static boolean deniedOwner(String ownerKey) {
        return DENIED_OWNERS.contains(ownerKey);
    }

    static boolean punishmentVerb(String root) {
        return PUNISHMENT_VERBS.contains(root);
    }

    static Optional<Set<String>> pluginPunishments(String ownerKey) {
        return Optional.ofNullable(PLUGIN_PUNISHMENTS.get(ownerKey));
    }

    private static Set<String> punishmentVerbs() {
        Set<String> verbs = new HashSet<>(VANILLA_PUNISHMENTS);
        PLUGIN_PUNISHMENTS.values().forEach(verbs::addAll);
        return Set.copyOf(verbs);
    }
}
