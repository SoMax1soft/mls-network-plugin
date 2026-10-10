// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.telemetry;

import java.util.Optional;
import java.util.UUID;

public interface ClientBrands {

    ClientBrands NONE = player -> Optional.empty();

    Optional<String> brand(UUID player);
}
