// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record NametagIdentity(int entityNumber, UUID uuid) {

    static final long UUID_HIGH_BITS = new BigInteger(1, "MLSAC".getBytes(StandardCharsets.US_ASCII)).longValueExact();

    public static NametagIdentity of(int entityNumber) {
        return new NametagIdentity(entityNumber, new UUID(UUID_HIGH_BITS, entityNumber));
    }
}
