/*
 * MLSAC is a GPLv3 licensed fork of a Minecraft anti-cheat system.
 * This project is community-maintained and not affiliated with any single upstream repository.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This file is based on GPLv3 licensed work and includes modifications.
 * Derived from:
 *   - SlothAC (© 2025 KaelusMC, https://github.com/KaelusMC/SlothAC)
 *   - Grim (© 2025 GrimAnticheat, https://github.com/GrimAnticheat/Grim)
 *   - Client-side project (GPLv3: https://github.com/MLSAC/client-side)
 */

package wtf.mlsac.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import wtf.mlsac.Main;
import wtf.mlsac.Permissions;
import wtf.mlsac.alert.AlertManager;
import wtf.mlsac.checks.AICheck;
import wtf.mlsac.config.Config;
import wtf.mlsac.config.Label;
import wtf.mlsac.data.AIPlayerData;
import wtf.mlsac.data.DataSession;
import wtf.mlsac.data.TickData;
import wtf.mlsac.scheduler.ScheduledTask;
import wtf.mlsac.scheduler.SchedulerManager;
import wtf.mlsac.session.ISessionManager;
import wtf.mlsac.util.ColorUtil;
import wtf.mlsac.util.ProbabilityFormatUtil;
import wtf.mlsac.violation.ViolationManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final ISessionManager sessionManager;
    private final AlertManager alertManager;
    private final AICheck aiCheck;
    private final Main plugin;
    private final wtf.mlsac.datacollector.DataRestorer dataRestorer;
    private final Map<UUID, UUID> probTracking = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> probTasks = new ConcurrentHashMap<>();
    private final Map<String, Long> reinstallConfirmations = new ConcurrentHashMap<>();
    private static final long REINSTALL_CONFIRM_WINDOW_MILLIS = TimeUnit.SECONDS.toMillis(3);

    public CommandHandler(ISessionManager sessionManager, AlertManager alertManager,
            AICheck aiCheck, Main plugin) {
        this.sessionManager = sessionManager;
        this.alertManager = alertManager;
        this.aiCheck = aiCheck;
        this.plugin = plugin;
        this.dataRestorer = new wtf.mlsac.datacollector.DataRestorer(plugin);
    }

    private Config getConfig() {
        return plugin.getPluginConfig();
    }

    private String getPrefix() {
        return ColorUtil.colorize(plugin.getMessagesConfig().getPrefix());
    }

    private String msg(String key) {
        return ColorUtil.colorize(plugin.getMessagesConfig().getMessage(key));
    }

    private String msg(String key, String... replacements) {
        return ColorUtil.colorize(plugin.getMessagesConfig().getMessage(key, replacements));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "alerts":
                return handleAlerts(sender);
            case "monitor":
                return handleMonitor(sender);
            case "prob":
                return handleProb(sender, args);
            case "reload":
                return handleReload(sender);
            case "reinstall":
                return handleReinstall(sender);
            case "datastatus":
                return handleDataStatus(sender);
            case "kicklist":
                return handleKickList(sender, args);
            case "suspects":
                return handleSuspects(sender);
            case "punish":
                return handlePunish(sender, args);
            case "profile":
                return handleProfile(sender, args);
            case "falsepositive":
                return handleFalsePositive(sender, args);
            case "status":
                return handleStatus(sender);
            case "animation":
                return handleAnimation(sender, args);
            default:
                sender.sendMessage(getPrefix() + msg("unknown-command", "{ARGS}", args[0]));
                sendUsage(sender);
                return true;
        }
    }

    private boolean handleSuspects(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.ALERTS) && !player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        new wtf.mlsac.menu.SuspectsMenu(plugin, player).open();
        return true;
    }

    private boolean handleAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.ALERTS) && !player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        alertManager.toggleAlerts(player);
        return true;
    }

    private boolean handleMonitor(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(Permissions.ALERTS) && !player.hasPermission(Permissions.ADMIN)) {
            player.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        alertManager.toggleMonitor(player);
        return true;
    }

    private boolean handleProb(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getPrefix() + msg("players-only"));
            return true;
        }
        Player admin = (Player) sender;
        if (!admin.hasPermission(Permissions.PROB) && !admin.hasPermission(Permissions.ADMIN)) {
            admin.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        if (probTracking.containsKey(admin.getUniqueId())) {
            stopTracking(admin);
            admin.sendMessage(getPrefix() + msg("tracking-stopped"));
            return true;
        }
        if (args.length < 2) {
            admin.sendMessage(getPrefix() + msg("prob-usage"));
            return true;
        }
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            admin.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", playerName));
            return true;
        }
        startTracking(admin, target);
        admin.sendMessage(getPrefix() + msg("tracking-started", "{PLAYER}", target.getName()));
        return true;
    }

    private void startTracking(Player admin, Player target) {
        UUID adminId = admin.getUniqueId();
        UUID targetId = target.getUniqueId();
        String targetName = target.getName();
        stopTracking(admin);
        probTracking.put(adminId, targetId);
        ScheduledTask task = SchedulerManager.getAdapter().runEntitySyncRepeating(admin, () -> {
            Player adminPlayer = Bukkit.getPlayer(adminId);
            Player targetPlayer = Bukkit.getPlayer(targetId);
            if (adminPlayer == null || !adminPlayer.isOnline()) {
                stopTracking(adminId);
                return;
            }
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sendActionBar(adminPlayer, msg("player-offline"));
                stopTracking(adminId);
                return;
            }
            AIPlayerData data = aiCheck.getPlayerData(targetId);
            String message;
            if (data == null) {
                message = ColorUtil.colorize("&7" + targetName + ": &eНет данных");
            } else {
                double buffer = data.getBuffer();
                int vl = plugin.getViolationManager().getViolationLevel(targetId);
                String template = plugin.getMessagesConfig().getMessage("actionbar-format",
                        targetName, data.getLastProbability(), buffer, vl);
                template = ProbabilityFormatUtil.applyModelPlaceholders(template, data)
                        .replace("{PLAYER}", targetName);
                message = ColorUtil.colorize(template);
            }
            sendActionBar(adminPlayer, message);
        }, 0L, 10L);
        probTasks.put(adminId, task);
    }

    private void stopTracking(Player admin) {
        stopTracking(admin.getUniqueId());
    }

    private void stopTracking(UUID adminId) {
        probTracking.remove(adminId);
        ScheduledTask task = probTasks.remove(adminId);
        if (task != null) {
            task.cancel();
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(Permissions.RELOAD) && !sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        plugin.reloadPluginConfig();
        
        // Перезагрузка анимаций
        wtf.mlsac.penalty.engine.AnimationManager animationManager = plugin.getAnimationManager();
        if (animationManager != null) {
            try {
                animationManager.reload();
                sender.sendMessage(getPrefix() + ColorUtil.colorize("&aАнимации перезагружены! &7(" + 
                    animationManager.getAvailableAnimations().size() + " анимаций)"));
            } catch (Exception e) {
                sender.sendMessage(getPrefix() + ColorUtil.colorize("&cОшибка при перезагрузке анимаций!"));
                plugin.getLogger().severe("Failed to reload animations: " + e.getMessage());
            }
        }
        
        sender.sendMessage(getPrefix() + msg("config-reloaded"));
        return true;
    }

    private boolean handleReinstall(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        String confirmationKey = getConfirmationKey(sender);
        long now = System.currentTimeMillis();
        Long expiresAt = reinstallConfirmations.get(confirmationKey);
        if (expiresAt == null || expiresAt < now) {
            reinstallConfirmations.put(confirmationKey, now + REINSTALL_CONFIRM_WINDOW_MILLIS);
            sender.sendMessage(getPrefix() + ColorUtil.colorize(
                    "&eПовторно введите &f/mlsac reinstall &eв течение 3 секунд для подтверждения."));
            return true;
        }
        reinstallConfirmations.remove(confirmationKey);
        boolean success = plugin.reinstallPluginConfig();
        if (success) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize(
                    "&aYAML configs were synced with current defaults. Existing values were kept and missing fields were added."));
        } else {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&cFailed to sync YAML configs. Check console."));
        }
        return true;
    }

    private String getConfirmationKey(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId().toString();
        }
        return "console:" + sender.getName().toLowerCase();
    }

    private boolean handleKickList(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        List<ViolationManager.KickRecord> kicks = plugin.getViolationManager().getKickHistory();
        if (kicks.isEmpty()) {
            sender.sendMessage(getPrefix() + msg("kicklist-empty"));
            return true;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        int pageSize = 10;
        int maxPage = (int) Math.ceil((double) kicks.size() / pageSize);
        if (page < 1)
            page = 1;
        if (page > maxPage)
            page = maxPage;

        sender.sendMessage(getPrefix()
                + msg("kicklist-header", "{PAGE}", String.valueOf(page), "{MAX_PAGE}", String.valueOf(maxPage)));
        sender.sendMessage(ColorUtil.colorize("&7─────────────────────────────────"));

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, kicks.size());

        for (int i = start; i < end; i++) {
            ViolationManager.KickRecord kick = kicks.get(i);
            sender.sendMessage(ColorUtil.colorize(String.format(
                    "&e%d. &f%s &7[&c%s&7] &8- &bProb: &f%.2f &8| &bBuf: &f%.1f &8| &bVL: &f%d",
                    i + 1,
                    kick.getPlayerName(),
                    kick.getFormattedTime(),
                    kick.getProbability(),
                    kick.getBuffer(),
                    kick.getVl())));
        }
        sender.sendMessage(ColorUtil.colorize("&7─────────────────────────────────"));
        if (page < maxPage) {
            sender.sendMessage(getPrefix() + msg("kicklist-footer", "{NEXT_PAGE}", String.valueOf(page + 1)));
        }
        return true;
    }

    private boolean handleFalsePositive(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("restore")) {
            sender.sendMessage(getPrefix() + msg("falsepositive-usage"));
            return true;
        }

        String targetName = args[2];
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", targetName));
            return true;
        }

        AIPlayerData data = aiCheck.getPlayerData(target.getUniqueId());
        if (data == null) {
            sender.sendMessage(getPrefix() + msg("falsepositive-no-data"));
            return true;
        }

        List<TickData> history = data.getTickHistory();
        boolean success = dataRestorer.restoreData(target.getName(), history);

        if (success) {
            sender.sendMessage(getPrefix() + msg("falsepositive-success"));
        } else {
            sender.sendMessage(getPrefix() + msg("falsepositive-fail"));
        }
        return true;
    }

    private boolean handlePunish(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(getPrefix() + msg("usage-punish"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", args[1]));
            return true;
        }

        plugin.getViolationManager().executeMaxPunishment(target);
        if (plugin.getPluginConfig().getPunishmentCommands().isEmpty()) {
            sender.sendMessage(getPrefix() + msg("punish-no-action"));
        } else {
            sender.sendMessage(getPrefix() + msg("punish-success", "{PLAYER}", target.getName(), "{ACTION}", "Max VL"));
        }
        return true;
    }

    private boolean handleProfile(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(getPrefix() + msg("usage-profile"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", args[1]));
            return true;
        }

        AIPlayerData data = aiCheck.getPlayerData(target.getUniqueId());
        String sens = "N/A";
        int detections = 0;

        if (data != null) {
            int s = data.getAimProcessor().getSensitivity();
            if (s != -1) {
                sens = String.valueOf(s);
            }
            detections = data.getHighProbabilityDetections();
        }

        ClientVersion version = PacketEvents.getAPI().getPlayerManager().getClientVersion(target);
        String clientVer = version != null ? version.toString() : "Unknown";

        sender.sendMessage(ColorUtil.colorize(msg("profile-header", "{PLAYER}", target.getName())));
        List<String> info = plugin.getMessagesConfig().getMessageList("profile-info");
        if (info == null || info.isEmpty()) {
            sender.sendMessage(ColorUtil.colorize("&7Sens: &f" + sens + "%"));
            sender.sendMessage(ColorUtil.colorize("&7Client: &f" + clientVer));
            sender.sendMessage(ColorUtil.colorize("&7Detections (>0.8): &f" + detections));
        } else {
            for (String line : info) {
                sender.sendMessage(ColorUtil.colorize(line
                        .replace("{PLAYER}", target.getName())
                        .replace("{SENS}", sens)
                        .replace("{CLIENT}", clientVer)
                        .replace("{DETECTIONS}", String.valueOf(detections))));
            }
        }

        return true;
    }

    private boolean handleDataStatus(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        int activeSessions = sessionManager.getActiveSessionCount();
        sender.sendMessage(getPrefix() + msg("data-status-header"));
        sender.sendMessage(msg("active-sessions", "{COUNT}", String.valueOf(activeSessions)));
        if (activeSessions > 0) {
            sender.sendMessage(ColorUtil.colorize("&7Игроки собирающие данные:"));
            for (DataSession session : sessionManager.getActiveSessions()) {
                Player player = Bukkit.getPlayer(session.getUuid());
                String playerName = player != null ? player.getName() : session.getPlayerName();
                String sessionLabel = session.getLabel().name();
                String comment = session.getComment();
                boolean inCombat = session.isInCombat();
                int tickCount = session.getTickCount();
                sender.sendMessage(ColorUtil.colorize("&b  " + playerName + "&7 [&e" + sessionLabel + "&7]" +
                        (comment.isEmpty() ? "" : " \"" + comment + "\"")));
                sender.sendMessage(ColorUtil.colorize("&7    Тики: &a" + tickCount +
                        "&7 | В бою: " + (inCombat ? "&aДа" : "&cНет")));
            }
        } else {
            sender.sendMessage(msg("no-active-sessions"));
            sender.sendMessage(msg("start-hint"));
        }
        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(getPrefix() + msg("usage-start"));
            return true;
        }
        String target = args[1];
        String labelStr = args[2];
        Label sessionLabel = Label.fromString(labelStr);
        if (sessionLabel == null) {
            sender.sendMessage(getPrefix() + msg("invalid-label", "{LABEL}", labelStr));
            sender.sendMessage(getPrefix() + msg("valid-labels"));
            return true;
        }
        String comment = parseComment(args, 3);
        return handleStartPlayer(sender, target, sessionLabel, comment);
    }

    private boolean handleStartPlayer(CommandSender sender, String playerName, Label label, String comment) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", playerName));
            return true;
        }
        sessionManager.startSession(player, label, comment);
        sender.sendMessage(getPrefix() + msg("session-started", "{LABEL}", label.name(), "{COUNT}", "1"));
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(getPrefix() + msg("usage-stop"));
            return true;
        }
        String target = args[1];
        if (target.equalsIgnoreCase("all")) {
            return handleStopAll(sender);
        }
        return handleStopPlayer(sender, target);
    }

    private boolean handleStopAll(CommandSender sender) {
        int count = sessionManager.getActiveSessionCount();
        sessionManager.stopAllSessions();
        sender.sendMessage(getPrefix() + msg("all-sessions-stopped", "{COUNT}", String.valueOf(count)));
        return true;
    }

    private boolean handleStopPlayer(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            if (!sessionManager.hasActiveSession(player)) {
                sender.sendMessage(getPrefix() + msg("no-sessions-to-stop"));
                return true;
            }
            sessionManager.stopSession(player);
            sender.sendMessage(getPrefix() + msg("session-stopped", "{PLAYER}", player.getName()));
            return true;
        }

        DataSession targetSession = null;
        for (DataSession session : sessionManager.getActiveSessions()) {
            if (session.getPlayerName().equalsIgnoreCase(playerName)) {
                targetSession = session;
                break;
            }
        }

        if (targetSession != null) {
            sender.sendMessage(getPrefix()
                    + ColorUtil.colorize("&cOffline stopping not fully supported without SessionManager update."));
            return true;
        }

        sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", playerName));
        return true;
    }

    private String parseComment(String[] args, int startIndex) {
        if (startIndex >= args.length) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(args[i]);
        }
        String comment = sb.toString();
        if (comment.startsWith("\"") && comment.endsWith("\"") && comment.length() >= 2) {
            comment = comment.substring(1, comment.length() - 1);
        } else if (comment.startsWith("\"")) {
            comment = comment.substring(1);
        }
        return comment.trim();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(getPrefix() + msg("usage-header"));
        sender.sendMessage(msg("usage-start"));
        sender.sendMessage(msg("usage-stop"));
        sender.sendMessage(msg("usage-datastatus"));
        sender.sendMessage(msg("usage-alerts"));
        sender.sendMessage(ColorUtil.colorize("&7  /mlsac monitor - Toggle monitor mode (show all detections)"));
        sender.sendMessage(msg("usage-prob"));
        sender.sendMessage(msg("usage-suspects"));
        sender.sendMessage(msg("usage-punish"));
        sender.sendMessage(msg("usage-profile"));
        sender.sendMessage(ColorUtil.colorize("&7  /mlsac reload - Reload config and animations"));
        sender.sendMessage(ColorUtil.colorize("&7  /mlsac status - Check API connection, latency, and daily stats"));
        sender.sendMessage(ColorUtil.colorize("&7  /mlsac animation test <player> <animation> - Test ban animation"));
        sender.sendMessage(ColorUtil.colorize("&7  /mlsac animation list - List available animations"));
        sender.sendMessage(ColorUtil.colorize("&7  /mlsac animation reload - Reload only animations"));
        sender.sendMessage(ColorUtil
                .colorize("&7  /mlsac reinstall - Add missing fields to config, messages, menu, and holograms"));
        sender.sendMessage(ColorUtil.colorize("&7  /mlsac kicklist [page] - Список киков от AI античита"));
        sender.sendMessage(
                ColorUtil.colorize("&7  /mlsac falsepositive restore <player> - Сохранить 5000 тиков игрока в CSV"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> commands = Arrays.asList("start", "stop", "datastatus", "alerts", "monitor", "prob", "reload",
                    "reinstall", "kicklist", "suspects", "punish", "profile", "falsepositive", "status", "animation");
            completions.addAll(filterStartsWith(commands, args[0]));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("start", "stop", "prob", "punish", "profile").contains(subCommand)) {
                List<String> targets = new ArrayList<>(getOnlinePlayerNames());
                if (subCommand.equals("stop"))
                    targets.add("all");
                completions.addAll(filterStartsWith(targets, args[1]));
            } else if (subCommand.equals("falsepositive")) {
                completions.add("restore");
            } else if (subCommand.equals("animation")) {
                completions.addAll(filterStartsWith(Arrays.asList("test", "list", "reload"), args[1]));
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equalsIgnoreCase("falsepositive") && args[1].equalsIgnoreCase("restore")) {
                completions.addAll(filterStartsWith(getOnlinePlayerNames(), args[2]));
            } else if (args[0].equalsIgnoreCase("start")) {
                List<String> labels = Arrays.stream(Label.values())
                        .map(Label::name)
                        .collect(Collectors.toList());
                completions.addAll(filterStartsWith(labels, args[2]));
            } else if (subCommand.equalsIgnoreCase("animation") && args[1].equalsIgnoreCase("test")) {
                completions.addAll(filterStartsWith(getOnlinePlayerNames(), args[2]));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("start")) {
                if (args[3].isEmpty() || args[3].startsWith("\"")) {
                    completions.add("\"comment\"");
                }
            } else if (args[0].equalsIgnoreCase("animation") && args[1].equalsIgnoreCase("test")) {
                wtf.mlsac.penalty.engine.AnimationManager animationManager = plugin.getAnimationManager();
                if (animationManager != null) {
                    completions.addAll(filterStartsWith(
                        new ArrayList<>(animationManager.getAvailableAnimations()), 
                        args[3]
                    ));
                }
            }
        }
        return completions;
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerPrefix))
                .collect(Collectors.toList());
    }

    public void cleanup() {
        for (ScheduledTask task : probTasks.values()) {
            task.cancel();
        }
        probTasks.clear();
        probTracking.clear();
        reinstallConfirmations.clear();
    }

    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }

        sender.sendMessage(ColorUtil.colorize("&8=== &7&lMLSAC STATUS &8==="));

        // API Connection Status
        wtf.mlsac.server.AIClientProvider provider = plugin.getAiClientProvider();
        wtf.mlsac.server.IAIClient client = provider != null ? provider.get() : null;

        if (client == null) {
            sender.sendMessage(ColorUtil.colorize("&7API: &cNot initialized"));
        } else {
            boolean connected = client.isConnected();
            boolean limitExceeded = client.isLimitExceeded();
            boolean serverError = client.isServerErrorState();
            boolean stasisMode = client.isInStasisMode();

            String status;
            if (stasisMode) {
                status = "&eStasis Mode (Rate Limited)";
            } else if (connected && !limitExceeded && !serverError) {
                status = "&aConnected";
            } else if (limitExceeded) {
                status = "&eRate Limited";
            } else if (serverError) {
                status = "&cServer Error";
            } else {
                status = "&cDisconnected";
            }

            sender.sendMessage(ColorUtil.colorize("&7API: " + status));
            sender.sendMessage(ColorUtil.colorize(
                    "&7Backend: &f" + (client.getServerAddress() != null ? client.getServerAddress() : "N/A")));
        }

        // Daily Statistics
        wtf.mlsac.stats.DailyStats stats = plugin.getDailyStats();
        if (stats != null) {
            sender.sendMessage(ColorUtil.colorize("&7Today's Detections: &f" + stats.getTodayDetections()));
            sender.sendMessage(ColorUtil.colorize("&7Today's Requests: &f" + stats.getTodayRequests()));
        }

        // Latency Test
        if (client != null && client.isConnected() && !client.isInStasisMode()) {
            java.util.List<java.util.concurrent.CompletableFuture<Long>> latencyTests = new java.util.ArrayList<>();
            for (int i = 0; i < 5; i++) {
                latencyTests.add(client.measureLatency());
            }

            java.util.concurrent.CompletableFuture
                    .allOf(latencyTests.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .thenAccept(v -> {
                        java.util.List<Long> results = new java.util.ArrayList<>();
                        for (java.util.concurrent.CompletableFuture<Long> future : latencyTests) {
                            try {
                                Long latency = future.get();
                                if (latency > 0) {
                                    results.add(latency);
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        if (results.isEmpty()) {
                            sender.sendMessage(ColorUtil.colorize("&7Average Latency: &cFailed to measure"));
                        } else {
                            long sum = 0;
                            for (Long l : results) {
                                sum += l;
                            }
                            long avg = sum / results.size();

                            String latencyColor;
                            if (avg < 50) {
                                latencyColor = "&a";
                            } else if (avg < 150) {
                                latencyColor = "&e";
                            } else {
                                latencyColor = "&c";
                            }

                            sender.sendMessage(ColorUtil.colorize("&7Average Latency: " + latencyColor + avg + "ms &7("
                                    + results.size() + "/5 successful)"));
                        }
                    })
                    .exceptionally(ex -> {
                        sender.sendMessage(ColorUtil.colorize("&7Average Latency: &cError during test"));
                        return null;
                    });
        } else {
            sender.sendMessage(ColorUtil.colorize("&7Average Latency: &cAPI not available"));
        }

        return true;
    }
    
    private boolean handleAnimation(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            sender.sendMessage(getPrefix() + msg("no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&cИспользование:"));
            sender.sendMessage(ColorUtil.colorize("&7  /mlsac animation test <игрок> <анимация> - Тест анимации"));
            sender.sendMessage(ColorUtil.colorize("&7  /mlsac animation list - Список доступных анимаций"));
            sender.sendMessage(ColorUtil.colorize("&7  /mlsac animation reload - Перезагрузить анимации"));
            return true;
        }
        
        String subCmd = args[1].toLowerCase();
        
        switch (subCmd) {
            case "test":
                return handleAnimationTest(sender, args);
            case "list":
                return handleAnimationList(sender);
            case "reload":
                return handleAnimationReload(sender);
            default:
                sender.sendMessage(getPrefix() + ColorUtil.colorize("&cНеизвестная подкоманда: " + args[1]));
                return true;
        }
    }
    
    private boolean handleAnimationTest(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&cИспользование: /mlsac animation test <игрок> <анимация>"));
            return true;
        }
        
        String playerName = args[2];
        String animationName = args[3];
        
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(getPrefix() + msg("player-not-found", "{PLAYER}", playerName));
            return true;
        }
        
        wtf.mlsac.penalty.engine.AnimationManager animationManager = plugin.getAnimationManager();
        if (animationManager == null) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&cAnimation Manager не инициализирован!"));
            return true;
        }
        
        if (!animationManager.hasAnimation(animationName)) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&cАнимация &f" + animationName + " &cне найдена!"));
            sender.sendMessage(ColorUtil.colorize("&7Используйте &f/mlsac animation list &7для списка доступных анимаций."));
            return true;
        }
        
        // Тестовая команда (не выполняется)
        String testCommand = "say " + target.getName() + " прошел тест анимации " + animationName;
        
        boolean success = animationManager.playAnimation(target, animationName, testCommand);
        
        if (success) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&aЗапущена анимация &f" + animationName + " &aдля игрока &f" + target.getName()));
            sender.sendMessage(ColorUtil.colorize("&7Это тестовый режим - команда бана не будет выполнена."));
        } else {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&cНе удалось запустить анимацию!"));
        }
        
        return true;
    }
    
    private boolean handleAnimationList(CommandSender sender) {
        wtf.mlsac.penalty.engine.AnimationManager animationManager = plugin.getAnimationManager();
        if (animationManager == null) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&cAnimation Manager не инициализирован!"));
            return true;
        }
        
        java.util.Set<String> animations = animationManager.getAvailableAnimations();
        
        sender.sendMessage(ColorUtil.colorize("&8=== &7&lДОСТУПНЫЕ АНИМАЦИИ &8==="));
        sender.sendMessage(ColorUtil.colorize("&7Всего анимаций: &f" + animations.size()));
        sender.sendMessage(ColorUtil.colorize("&7Текущая по умолчанию: &a" + animationManager.getDefaultAnimationName()));
        sender.sendMessage(ColorUtil.colorize("&7─────────────────────────────────"));
        
        for (String name : animations) {
            boolean isDefault = name.equals(animationManager.getDefaultAnimationName());
            String prefix = isDefault ? "&a● " : "&7• ";
            sender.sendMessage(ColorUtil.colorize(prefix + "&f" + name + (isDefault ? " &7(по умолчанию)" : "")));
        }
        
        sender.sendMessage(ColorUtil.colorize("&7─────────────────────────────────"));
        sender.sendMessage(ColorUtil.colorize("&7Используйте &f/mlsac animation test <игрок> <анимация> &7для теста"));
        
        return true;
    }
    
    private boolean handleAnimationReload(CommandSender sender) {
        wtf.mlsac.penalty.engine.AnimationManager animationManager = plugin.getAnimationManager();
        if (animationManager == null) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&cAnimation Manager не инициализирован!"));
            return true;
        }
        
        try {
            animationManager.reload();
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&aАнимации успешно перезагружены!"));
            sender.sendMessage(ColorUtil.colorize("&7Загружено анимаций: &f" + animationManager.getAvailableAnimations().size()));
        } catch (Exception e) {
            sender.sendMessage(getPrefix() + ColorUtil.colorize("&cОшибка при перезагрузке анимаций!"));
            plugin.getLogger().severe("Failed to reload animations: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
}
