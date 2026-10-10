// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.banimation;

import java.util.Map;
import java.util.function.Supplier;

public record AnimationFile(String fileName, Supplier<Map<?, ?>> document) {
}
