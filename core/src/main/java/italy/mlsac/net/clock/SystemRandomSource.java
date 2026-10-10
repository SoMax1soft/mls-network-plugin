// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.clock;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class SystemRandomSource implements RandomSource {

    @Override
    public long nextLong(String purpose, long bound) {
        return ThreadLocalRandom.current().nextLong(bound);
    }

    @Override
    public double nextDouble(String purpose) {
        return ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public UUID randomUuid(String purpose) {
        return UUID.randomUUID();
    }
}
