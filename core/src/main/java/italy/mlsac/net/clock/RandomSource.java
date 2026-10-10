// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.clock;

import java.util.UUID;

public interface RandomSource {

    long nextLong(String purpose, long bound);

    double nextDouble(String purpose);

    UUID randomUuid(String purpose);
}
