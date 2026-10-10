// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LegacyWindowEncoderTest {

    @Test
    void allZeroTicksShareTheEmptyVtable() {
        byte[] bytes = LegacyWindowEncoder.encode(Collections.nCopies(40, LegacyDelta.ZERO));
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int root = buffer.getInt(0);
        int vectorField = root + fieldOffset(buffer, root, 0);
        int vector = vectorField + buffer.getInt(vectorField);
        int length = buffer.getInt(vector);
        assertEquals(40, length);
        Set<Integer> vtables = new HashSet<>();
        for (int element = 0; element < length; element++) {
            int slot = vector + 4 + 4 * element;
            int table = slot + buffer.getInt(slot);
            int vtable = table - buffer.getInt(table);
            vtables.add(vtable);
            assertEquals(4, buffer.getShort(vtable));
            assertEquals(4, buffer.getShort(vtable + 2));
        }
        assertEquals(1, vtables.size());
    }

    @Test
    void storesNonZeroValuesInFieldOrder() {
        List<LegacyDelta> ticks = new ArrayList<>(Collections.nCopies(39, LegacyDelta.ZERO));
        ticks.add(new LegacyDelta(1f, 2f, 0f, -0f, 5f, Float.NaN, 7f, 8f));
        ByteBuffer buffer = ByteBuffer.wrap(LegacyWindowEncoder.encode(ticks)).order(ByteOrder.LITTLE_ENDIAN);
        int root = buffer.getInt(0);
        int vectorField = root + fieldOffset(buffer, root, 0);
        int vector = vectorField + buffer.getInt(vectorField);
        int slot = vector + 4 + 4 * 39;
        int table = slot + buffer.getInt(slot);
        assertEquals(1f, buffer.getFloat(table + fieldOffset(buffer, table, 0)));
        assertEquals(2f, buffer.getFloat(table + fieldOffset(buffer, table, 1)));
        assertEquals(0, fieldOffset(buffer, table, 2));
        assertEquals(0, fieldOffset(buffer, table, 3));
        assertEquals(5f, buffer.getFloat(table + fieldOffset(buffer, table, 4)));
        assertEquals(Float.NaN, buffer.getFloat(table + fieldOffset(buffer, table, 5)));
    }

    private static int fieldOffset(ByteBuffer buffer, int table, int field) {
        int vtable = table - buffer.getInt(table);
        int vtableSize = buffer.getShort(vtable);
        int entry = 4 + 2 * field;
        return entry < vtableSize ? buffer.getShort(vtable + entry) : 0;
    }
}
