// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.commands;

import java.util.List;
import java.util.Optional;

public interface PlayerLookup {

    Optional<Target> byNamePrefix(String name);

    Optional<Target> byExactName(String name);

    List<String> onlineNames();
}
