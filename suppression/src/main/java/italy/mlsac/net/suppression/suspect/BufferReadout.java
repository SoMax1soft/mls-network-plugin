// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.suspect;

import java.util.OptionalDouble;
import java.util.UUID;

@FunctionalInterface
public interface BufferReadout {

    OptionalDouble currentBuffer(UUID suspectId);
}
