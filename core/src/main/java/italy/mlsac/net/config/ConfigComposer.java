// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.Optional;

public final class ConfigComposer implements PresetSink {

    private static final String SUPPRESSION_PREFIX = "suppression";

    private final ConfigAssembler assembler;
    private final ConfigReference reference;
    private final EventBus bus;
    private final PluginLog log;
    private ConfigTree localFile = ConfigTree.empty();
    private CoreSettings core;
    private Optional<PresetSnapshot> snapshot = Optional.empty();

    public ConfigComposer(ConfigAssembler assembler, ConfigReference reference, EventBus bus, PluginLog log) {
        this.assembler = assembler;
        this.reference = reference;
        this.bus = bus;
        this.log = log;
    }

    public synchronized EffectiveConfig loadLocal(ConfigTree file, CoreSettings settings, ConfigChangeCause cause) {
        this.localFile = file;
        this.core = settings;
        return rebuild(cause);
    }

    @Override
    public synchronized void apply(Optional<PresetSnapshot> preset) {
        this.snapshot = preset;
        try {
            rebuild(ConfigChangeCause.PRESET);
        } catch (RuntimeException failure) {
            log.error(LogTopic.PRESET, "Preset not applied: " + failure.getMessage(), failure);
        }
    }

    public synchronized Optional<PresetSnapshot> lastSnapshot() {
        return snapshot;
    }

    private EffectiveConfig rebuild(ConfigChangeCause cause) {
        ConfigAssembler.AssembledConfig assembled = assembler.assemble(core, localFile, snapshot);
        report(assembled);
        EffectiveConfig previous = reference.get();
        reference.publish(assembled.config());
        bus.publish(new ConfigApplied(previous, assembled.config(), cause));
        return assembled.config();
    }

    private void report(ConfigAssembler.AssembledConfig assembled) {
        for (ConfigIssue issue : assembled.issues()) {
            if (issue.severity() == IssueSeverity.WARNING) {
                LogTopic topic = issue.path().startsWith(SUPPRESSION_PREFIX) ? LogTopic.SUPPRESSION : LogTopic.CONFIG;
                log.warn(topic, "Ignored invalid " + issue.path() + ": " + issue.reason());
            } else {
                log.debug(LogTopic.PRESET, () -> "dropped invalid " + issue.path() + " from the "
                        + (issue.source() == LayerSource.PRESET ? "preset" : "config.yml") + ": " + issue.reason());
            }
        }
    }
}
