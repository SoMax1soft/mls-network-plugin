// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.player.ClientVersion;
import java.util.Optional;

public record ClientQuirks(boolean modernDeltas, boolean skipsTicks, boolean freezesOnRotation) {

    public static ClientQuirks of(Optional<ClientVersion> version) {
        if (version.isEmpty()) {
            return new ClientQuirks(true, true, false);
        }
        ClientVersion known = version.get();
        boolean freezes = (known.above(1, 14, 4) && known.below(1, 20, 2))
                || (known.above(1, 21, 4) && known.below(1, 21, 9));
        return new ClientQuirks(known.atLeast(1, 19, 3), known.atLeast(1, 9, 0), freezes);
    }
}
