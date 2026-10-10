// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import java.io.IOException;

public interface DataFolder {
    boolean exists(String relativePath);

    void write(String relativePath, byte[] content) throws IOException;
}
