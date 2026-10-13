// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Optional;

public interface BedrockPrefix {

    BedrockPrefix NONE = Optional::empty;

    Optional<String> prefix();
}
