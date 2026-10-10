// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.environment;

import java.nio.charset.Charset;

public interface PlatformText {
    String lineSeparator();

    Charset defaultCharset();
}
