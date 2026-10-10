// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import java.util.UUID;

public interface UuidSource {
    UUID next(UuidPurpose purpose);
}
