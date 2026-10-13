// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

import italy.mlsac.net.config.alert.AlertReader;
import italy.mlsac.net.config.core.CoreSettings;
import italy.mlsac.net.config.core.CoreSettingsReader;
import italy.mlsac.net.config.detection.DetectionReader;
import italy.mlsac.net.config.hologram.HologramReader;
import italy.mlsac.net.config.menu.MenuReader;
import italy.mlsac.net.config.message.MessageReader;
import italy.mlsac.net.config.module.ModuleReader;
import italy.mlsac.net.config.penalty.PenaltyReader;
import italy.mlsac.net.config.response.AlertResponseReader;
import italy.mlsac.net.config.staff.StaffToolReader;
import italy.mlsac.net.config.suppression.SuppressionReader;
import italy.mlsac.net.config.violation.ViolationReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ConfigAssembler {

    private final DetectionReader detection = new DetectionReader();
    private final AlertReader alerts = new AlertReader();
    private final ViolationReader violation = new ViolationReader();
    private final PenaltyReader penalties = new PenaltyReader();
    private final AlertResponseReader responses = new AlertResponseReader();
    private final SuppressionReader suppression = new SuppressionReader();
    private final ModuleReader modules = new ModuleReader();
    private final HologramReader holograms = new HologramReader();
    private final MenuReader menus = new MenuReader();
    private final MessageReader messages = new MessageReader();
    private final StaffToolReader staffTools = new StaffToolReader();

    public AssembledConfig assemble(CoreSettings core, ConfigTree localFile, Optional<PresetSnapshot> preset) {
        List<ConfigIssue> issues = new ArrayList<>();
        EffectiveConfig config = EffectiveConfig.pluginDefaults(core);
        IssueLog localIssues = new IssueLog();
        config = apply(config, new SettingsLayer(LayerSource.LOCAL_FILE, localFile, localIssues));
        issues.addAll(localIssues.issues());
        int presetAccepted = 0;
        if (preset.isPresent()) {
            IssueLog presetIssues = new IssueLog();
            config = apply(config, new SettingsLayer(LayerSource.PRESET, preset.get().effectiveLayout(), presetIssues));
            config = withPreset(config, preset.get().state());
            issues.addAll(presetIssues.issues());
            presetAccepted = presetIssues.acceptedValues();
        }
        return new AssembledConfig(config, issues, presetAccepted);
    }

    public int countUsableValues(ConfigTree presetLayout) {
        IssueLog log = new IssueLog();
        apply(EffectiveConfig.pluginDefaults(placeholderCore()), new SettingsLayer(LayerSource.PRESET, presetLayout, log));
        return log.acceptedValues();
    }

    private EffectiveConfig apply(EffectiveConfig lower, SettingsLayer layer) {
        return new EffectiveConfig(lower.core(),
                detection.read(lower.detection(), layer),
                alerts.read(lower.alerts(), layer),
                violation.read(lower.violation(), layer),
                penalties.read(lower.penalties(), layer),
                responses.read(lower.alertResponses(), layer),
                suppression.read(lower.suppression(), layer),
                modules.read(lower.modules(), layer),
                holograms.read(lower.holograms(), layer),
                menus.read(lower.menus(), layer),
                messages.read(lower.messages(), layer),
                staffTools.restore(lower.dataRestore(), layer),
                staffTools.dossier(lower.dossier(), layer),
                lower.preset());
    }

    private static EffectiveConfig withPreset(EffectiveConfig config, PresetState state) {
        return new EffectiveConfig(config.core(), config.detection(), config.alerts(), config.violation(),
                config.penalties(), config.alertResponses(), config.suppression(), config.modules(),
                config.holograms(), config.menus(), config.messages(), config.dataRestore(), config.dossier(), state);
    }

    private static CoreSettings placeholderCore() {
        return new CoreSettingsReader().read(ConfigTree.empty()).settings();
    }

    public record AssembledConfig(EffectiveConfig config, List<ConfigIssue> issues, int presetAcceptedValues) {

        public AssembledConfig {
            issues = List.copyOf(issues);
        }
    }
}
