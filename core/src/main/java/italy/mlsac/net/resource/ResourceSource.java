// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.resource;

import java.util.Optional;

public interface ResourceSource {

    Optional<String> text(String path);

    Optional<byte[]> bytes(String path);
}
