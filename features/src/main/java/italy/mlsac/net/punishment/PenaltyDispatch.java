// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.Optional;
import java.util.UUID;

public interface PenaltyDispatch {

    void console(String resolvedCommand);

    void staffNotice(String resolvedText);

    void animated(UUID player, String resolvedCommand, Optional<String> animationId);
}
