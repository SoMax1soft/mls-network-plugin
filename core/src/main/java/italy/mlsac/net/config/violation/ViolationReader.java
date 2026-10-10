// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.violation;

import italy.mlsac.net.config.LayerSource;
import italy.mlsac.net.config.SettingsLayer;
import java.util.Optional;
import java.util.OptionalDouble;

public final class ViolationReader {

    private static final double LEGACY_DECAY_THRESHOLD = 0.10;

    public ViolationSettings read(ViolationSettings lower, SettingsLayer layer) {
        OptionalDouble threshold = layer.clamped("violation.threshold", 1.0, 100_000.0);
        OptionalDouble reset = layer.clamped("violation.reset-value", 0.0, 100_000.0);
        double effectiveThreshold = threshold.orElse(lower.threshold());
        double effectiveReset = reset.orElse(lower.resetValue());
        if (layer.source() == LayerSource.PRESET && threshold.isPresent() && reset.isPresent()
                && effectiveReset >= effectiveThreshold) {
            effectiveReset = effectiveThreshold / 2.0;
        }
        double multiplier = layer.clamped("violation.multiplier", 0.0, 10_000.0).orElse(lower.multiplier());
        return new ViolationSettings(effectiveThreshold, effectiveReset, multiplier,
                readDecay(lower.decay(), layer), readVlDecay(lower.vlDecay(), layer));
    }

    private BufferDecay readDecay(BufferDecay lower, SettingsLayer layer) {
        Optional<Object> raw = layer.raw("violation.decay");
        if (raw.isPresent() && raw.get() instanceof Number && layer.source() == LayerSource.LOCAL_FILE) {
            double amount = layer.clamped("violation.decay", 0.0, 10_000.0).orElse(lower.amount());
            return new BufferDecay(LEGACY_DECAY_THRESHOLD, amount);
        }
        return new BufferDecay(
                layer.clamped("violation.decay.threshold", 0.0, 1.0).orElse(lower.threshold()),
                layer.clamped("violation.decay.amount", 0.0, 10_000.0).orElse(lower.amount()));
    }

    private ViolationDecay readVlDecay(ViolationDecay lower, SettingsLayer layer) {
        return new ViolationDecay(
                layer.bool("violation.vl-decay.enabled").orElse(lower.enabled()),
                layer.intClamped("violation.vl-decay.interval", 1, 86_400).orElse(lower.everySeconds()),
                layer.intClamped("violation.vl-decay.amount", 0, 10_000).orElse(lower.amount()));
    }
}
