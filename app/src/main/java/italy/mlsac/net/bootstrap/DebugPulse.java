// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.bootstrap;

import com.github.retrooper.packetevents.PacketEvents;
import italy.mlsac.net.backend.session.BackendStatus;
import italy.mlsac.net.config.EffectiveConfig;
import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.packet.packetevents.PacketEventsLayer;
import italy.mlsac.net.platform.PlatformProfile;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;

final class DebugPulse implements DebugReport {

    private static final long EVERY_TICKS = 200L;
    private static final Map<String, String> GROUPS = new LinkedHashMap<>();

    static {
        GROUPS.put("in.", "Packets in");
        GROUPS.put("out.", "Packets out");
        GROUPS.put("drop.", "Packets dropped");
        GROUPS.put("tick.", "Ticks");
        GROUPS.put("player.", "Players");
        GROUPS.put("attack.", "Attacks");
        GROUPS.put("legacy.", "Pro/Mini");
        GROUPS.put("ultra.", "Ultra");
        GROUPS.put("backend.", "Backend");
        GROUPS.put("badpackets.", "Bad packets");
    }

    private final CoreWiring core;
    private final PlatformProfile platform;
    private final PacketEventsLayer packets;
    private TaskTicket task;
    private boolean wasOn;

    DebugPulse(CoreWiring core, PlatformProfile platform, PacketEventsLayer packets) {
        this.core = core;
        this.platform = platform;
        this.packets = packets;
    }

    void start() {
        DebugTally.traceSink(line -> core.log.info(LogTopic.DETECTION, "[Trace] " + line));
        boolean on = DebugTally.on(core.config.get().core().debug());
        DebugTally.counting(on);
        wasOn = on;
        if (on) {
            summary().forEach(line -> core.log.info(LogTopic.PLATFORM, "[Debug] " + line));
        }
        task = core.scheduler.submit(TaskLane.BACKGROUND, TaskTiming.repeating(EVERY_TICKS, EVERY_TICKS), this::beat);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        DebugTally.counting(false);
        DebugTally.traceSink(null);
    }

    @Override
    public void refreshNow() {
        boolean on = DebugTally.on(core.config.get().core().debug());
        DebugTally.counting(on);
        if (on && !wasOn) {
            summary().forEach(line -> core.log.info(LogTopic.PLATFORM, "[Debug] " + line));
        }
        wasOn = on;
    }

    @Override
    public List<String> summary() {
        List<String> lines = new ArrayList<>();
        lines.add("Server: " + Bukkit.getName() + " " + Bukkit.getVersion());
        lines.add("Bukkit version '" + Bukkit.getBukkitVersion() + "' -> MLSAC rules " + platform.serverVersion().version()
                + " (" + platform.serverVersion().mode().label() + ", recognised " + platform.serverVersion().recognised()
                + "), software " + platform.software().displayName() + ", Folia " + platform.folia()
                + ", tick-end event " + platform.tickEndEventAvailable() + ", Java " + platform.javaFeatureVersion());
        String library;
        try {
            library = PacketEvents.getAPI().getVersion() + ", server " + PacketEvents.getAPI().getServerManager().getVersion();
        } catch (RuntimeException | LinkageError failure) {
            library = "unknown (" + failure.getClass().getSimpleName() + ")";
        }
        lines.add("PacketEvents " + library + " -> MLSAC packet rules " + packets.serverVersion());
        lines.addAll(state());
        return lines;
    }

    private List<String> state() {
        EffectiveConfig config = core.config.get();
        BackendStatus status = core.backend.status();
        List<String> lines = new ArrayList<>();
        lines.add("Detection " + config.detection().enabled() + ", Ultra mode " + config.detection().ultraMode()
                + ", models " + config.detection().models().models().keySet().stream()
                        .filter(model -> config.detection().models().enabled(model)).toList()
                + ", regions switch " + config.detection().regions().enabled()
                + " entries " + config.detection().regions().entries().size());
        lines.add("Backend " + status.state() + ", connected " + status.connected() + ", stasis " + status.stasis()
                + ", started " + core.backend.exists() + ", usable " + core.backend.usable()
                + ", preset " + (config.preset().applied() ? config.preset().resolvedName().orElse("?") : "not applied")
                + " (configured '" + config.core().preset().configuredName() + "')");
        lines.add("Players online " + core.players.online().size()
                + (DebugTally.traced() == null ? "" : ", tracing " + DebugTally.tracedName()));
        return lines;
    }

    private void beat() {
        refreshNow();
        if (!DebugTally.counting()) {
            return;
        }
        Map<String, Long> counts = DebugTally.drain();
        Map<String, StringBuilder> grouped = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            String key = entry.getKey();
            String group = GROUPS.entrySet().stream().filter(g -> key.startsWith(g.getKey()))
                    .map(Map.Entry::getValue).findFirst().orElse("Other");
            String prefix = GROUPS.entrySet().stream().filter(g -> key.startsWith(g.getKey()))
                    .map(Map.Entry::getKey).findFirst().orElse("");
            StringBuilder line = grouped.computeIfAbsent(group, g -> new StringBuilder());
            if (line.length() > 0) {
                line.append(", ");
            }
            line.append(key.substring(prefix.length())).append(' ').append(entry.getValue());
        }
        core.log.info(LogTopic.GENERAL, "[Debug] ---- last 10 s ----");
        state().forEach(line -> core.log.info(LogTopic.GENERAL, "[Debug] " + line));
        if (grouped.isEmpty()) {
            core.log.info(LogTopic.GENERAL, "[Debug] no packets or events at all in the last 10 s");
        }
        grouped.forEach((group, line) -> core.log.info(LogTopic.GENERAL, "[Debug] " + group + ": " + line));
    }
}
