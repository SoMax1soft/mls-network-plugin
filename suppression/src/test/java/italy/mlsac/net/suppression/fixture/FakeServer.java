// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.fixture;

import italy.mlsac.net.suppression.effect.EffectSnapshot;
import italy.mlsac.net.suppression.lag.motion.SetbackTerrain;
import italy.mlsac.net.suppression.motion.WorldPoint;
import italy.mlsac.net.suppression.server.CommandDispatch;
import italy.mlsac.net.suppression.server.EffectWatcher;
import italy.mlsac.net.suppression.server.ExemptionProbe;
import italy.mlsac.net.suppression.server.LagExemptSuspects;
import italy.mlsac.net.suppression.server.ServerThreads;
import italy.mlsac.net.suppression.server.SuppressionNotices;
import italy.mlsac.net.suppression.server.SuppressionTexts;
import italy.mlsac.net.suppression.server.SuspectBodies;
import italy.mlsac.net.suppression.server.SuspectBody;
import italy.mlsac.net.suppression.server.SuspectStatus;
import italy.mlsac.net.suppression.trigger.TriggerContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FakeServer implements ServerThreads, SuspectStatus, ExemptionProbe, CommandDispatch,
        SuppressionNotices, SuppressionTexts, SuspectBodies, EffectWatcher, SetbackTerrain,
        LagExemptSuspects {

    private static final Set<String> DESTRUCTIVE = Set.of("kick", "ban", "tempban", "banip", "ban-ip", "ipban",
            "mute", "tempmute", "jail", "punish");

    private final Timeline timeline;
    private final Set<UUID> offline = new HashSet<>();
    private final Set<UUID> paused = new HashSet<>();
    private final Set<UUID> exempt = new HashSet<>();
    private final Set<UUID> recorded = new HashSet<>();
    private final Map<UUID, FakeBody> bodies = new HashMap<>();
    private final Map<String, String> overrides = new HashMap<>();
    private final Map<String, String> bundled = new HashMap<>(BuiltInTexts.english());
    private String prefix = "";
    private final List<RanCommand> commands = new ArrayList<>();
    private final List<String> subscriberLines = new ArrayList<>();
    private final List<String> staffLines = new ArrayList<>();
    private final List<String> consoleLines = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final Map<UUID, EffectSnapshot> snapshots = new HashMap<>();
    private int exemptionProbes;
    private boolean passable = true;
    private boolean chunkLoaded = true;

    public FakeServer(Timeline timeline) {
        this.timeline = timeline;
    }

    public FakeBody body(UUID id) {
        return bodies.computeIfAbsent(id, key -> new FakeBody(key, timeline));
    }

    public void goOffline(UUID id) {
        offline.add(id);
    }

    public void goOnline(UUID id) {
        offline.remove(id);
    }

    public void pause(UUID id) {
        paused.add(id);
    }

    public void unpause(UUID id) {
        paused.remove(id);
    }

    public void startRecording(UUID id) {
        recorded.add(id);
    }

    public void stopRecording(UUID id) {
        recorded.remove(id);
    }

    @Override
    public boolean lagExempt(UUID suspectId) {
        return recorded.contains(suspectId);
    }

    public void grantBypass(UUID id) {
        exempt.add(id);
    }

    public void override(String key, String text) {
        overrides.put(key, text);
    }

    public void prefix(String text) {
        prefix = text;
    }

    public void terrain(boolean isPassable, boolean isLoaded) {
        passable = isPassable;
        chunkLoaded = isLoaded;
    }

    public List<RanCommand> commands() {
        return commands;
    }

    public List<String> commandTexts() {
        return commands.stream().map(RanCommand::text).toList();
    }

    public List<String> subscriberLines() {
        return subscriberLines;
    }

    public List<String> staffLines() {
        return staffLines;
    }

    public List<String> consoleLines() {
        return consoleLines;
    }

    public List<String> warnings() {
        return warnings;
    }

    public int exemptionProbes() {
        return exemptionProbes;
    }

    public Optional<EffectSnapshot> lastSnapshot(UUID id) {
        return Optional.ofNullable(snapshots.get(id));
    }

    @Override
    public void onSuspectThread(UUID suspectId, Runnable whileOnline, Runnable whenOffline) {
        if (online(suspectId)) {
            whileOnline.run();
        } else {
            whenOffline.run();
        }
    }

    @Override
    public void onGlobalThread(Runnable task) {
        task.run();
    }

    @Override
    public boolean online(UUID suspectId) {
        return !offline.contains(suspectId);
    }

    @Override
    public boolean effectsPaused(UUID suspectId) {
        return paused.contains(suspectId);
    }

    @Override
    public boolean exempt(UUID suspectId) {
        exemptionProbes++;
        return exempt.contains(suspectId);
    }

    @Override
    public boolean punishing(String ladderText) {
        String trimmed = ladderText.trim();
        if (trimmed.startsWith("{ANIMATION}")) {
            return true;
        }
        if (trimmed.startsWith("{CUSTOM_ALERT}")) {
            return false;
        }
        String first = trimmed.split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (first.startsWith("/")) {
            first = first.substring(1);
        }
        int colon = first.indexOf(':');
        if (colon >= 0) {
            first = first.substring(colon + 1);
        }
        return DESTRUCTIVE.contains(first);
    }

    @Override
    public void run(String ladderText, TriggerContext context) {
        commands.add(new RanCommand(ladderText, context));
        timeline.note("cmd " + ladderText);
    }

    @Override
    public void toAlertSubscribers(String text) {
        subscriberLines.add(text);
    }

    @Override
    public void toStaffAndOps(String text) {
        staffLines.add(text);
    }

    @Override
    public void toConsole(String text) {
        consoleLines.add(text);
    }

    @Override
    public void warn(String line) {
        warnings.add(line);
    }

    @Override
    public Optional<String> presetOverride(String key) {
        return Optional.ofNullable(overrides.get(key));
    }

    @Override
    public String bundled(String key) {
        return bundled.getOrDefault(key, "");
    }

    @Override
    public String prefix() {
        return prefix;
    }

    @Override
    public Optional<SuspectBody> bodyOf(UUID suspectId) {
        return online(suspectId) ? Optional.of(body(suspectId)) : Optional.empty();
    }

    @Override
    public void effectsChanged(UUID suspectId, EffectSnapshot snapshot) {
        snapshots.put(suspectId, snapshot);
        timeline.note("effects " + suspectId);
    }

    @Override
    public boolean chunkLoaded(WorldPoint position) {
        return chunkLoaded;
    }

    @Override
    public boolean passable(WorldPoint position) {
        return passable;
    }

    public record RanCommand(String text, TriggerContext context) {
    }
}
