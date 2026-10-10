// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.Optional;
import java.util.Set;

public record ParticleLook(String particle, Optional<DustColor> dust) {

    private static final Set<String> DUST_NAMES = Set.of("DUST", "REDSTONE");

    public static ParticleLook of(String particle, boolean colored, int red, int green, int blue, double size) {
        Optional<DustColor> dust = colored && DUST_NAMES.contains(particle)
                ? Optional.of(DustColor.clamped(red, green, blue, size)) : Optional.empty();
        return new ParticleLook(particle, dust);
    }
}
