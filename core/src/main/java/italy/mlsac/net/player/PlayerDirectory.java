// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerDirectory {

    Optional<PlayerView> byId(UUID id);

    Optional<PlayerView> byNamePrefix(String name);

    Optional<PlayerView> byExactName(String name);

    List<PlayerView> online();
}
