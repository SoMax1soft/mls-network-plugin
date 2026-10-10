// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

public record ConfigApplied(EffectiveConfig previous, EffectiveConfig current, ConfigChangeCause cause) {
}
