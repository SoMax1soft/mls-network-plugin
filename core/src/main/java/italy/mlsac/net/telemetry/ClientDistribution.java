// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.telemetry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ClientDistribution(Map<String, Integer> protocols, Map<String, Integer> brands) {

    public static final ClientDistribution EMPTY = new ClientDistribution(Map.of(), Map.of());

    public ClientDistribution {
        protocols = Collections.unmodifiableMap(new LinkedHashMap<>(protocols));
        brands = Collections.unmodifiableMap(new LinkedHashMap<>(brands));
    }

    public boolean empty() {
        return protocols.isEmpty() && brands.isEmpty();
    }
}
