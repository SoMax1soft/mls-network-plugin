// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

public sealed interface MetaValue permits MetaValue.ByteValue, MetaValue.IntValue, MetaValue.FloatValue,
        MetaValue.BooleanValue, MetaValue.Vector3fValue, MetaValue.TextValue, MetaValue.OptionalTextValue {

    record ByteValue(byte value) implements MetaValue {
    }

    record IntValue(int value) implements MetaValue {
    }

    record FloatValue(float value) implements MetaValue {
    }

    record BooleanValue(boolean value) implements MetaValue {
    }

    record Vector3fValue(float x, float y, float z) implements MetaValue {
    }

    record TextValue(String sectionSignText) implements MetaValue {
    }

    record OptionalTextValue(String sectionSignText) implements MetaValue {
    }
}
