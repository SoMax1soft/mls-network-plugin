// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.Optional;

public sealed interface MetadataEntry {

    int index();

    record ByteValue(int index, byte value) implements MetadataEntry {
    }

    record IntValue(int index, int value) implements MetadataEntry {
    }

    record FloatValue(int index, float value) implements MetadataEntry {
    }

    record BooleanValue(int index, boolean value) implements MetadataEntry {
    }

    record VectorValue(int index, float x, float y, float z) implements MetadataEntry {
    }

    record TextValue(int index, String coloredText) implements MetadataEntry {
    }

    record OptionalTextValue(int index, Optional<String> coloredText) implements MetadataEntry {
    }
}
