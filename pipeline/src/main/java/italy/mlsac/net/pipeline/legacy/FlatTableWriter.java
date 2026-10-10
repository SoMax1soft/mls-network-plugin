// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.legacy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FlatTableWriter {
    private static final int INT_BYTES = 4;
    private static final int SHORT_BYTES = 2;
    private static final int STANDARD_VTABLE_FIELDS = 2;

    private final ByteBuffer buffer;
    private int space;
    private int alignment = 1;
    private int[] slots = new int[0];
    private int slotsInUse;
    private int tableStart;
    private int vectorLength;
    private final List<Integer> vtables = new ArrayList<>();

    public FlatTableWriter(int capacity) {
        this.buffer = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        this.space = capacity;
    }

    public int offset() {
        return buffer.capacity() - space;
    }

    private void pad(int count) {
        for (int index = 0; index < count; index++) {
            buffer.put(--space, (byte) 0);
        }
    }

    private void prep(int size, int additionalBytes) {
        if (size > alignment) {
            alignment = size;
        }
        int alignSize = ((~(buffer.capacity() - space + additionalBytes)) + 1) & (size - 1);
        if (space < alignSize + size + additionalBytes) {
            throw new IllegalStateException("flat table buffer too small");
        }
        pad(alignSize);
    }

    private void putInt(int value) {
        space -= INT_BYTES;
        buffer.putInt(space, value);
    }

    private void putShort(short value) {
        space -= SHORT_BYTES;
        buffer.putShort(space, value);
    }

    private void addInt(int value) {
        prep(INT_BYTES, 0);
        putInt(value);
    }

    private void addShort(short value) {
        prep(SHORT_BYTES, 0);
        putShort(value);
    }

    private void addOffset(int target) {
        prep(INT_BYTES, 0);
        putInt(offset() - target + INT_BYTES);
    }

    public void startTable(int fieldCount) {
        if (slots.length < fieldCount) {
            slots = new int[fieldCount];
        }
        slotsInUse = fieldCount;
        Arrays.fill(slots, 0, slotsInUse, 0);
        tableStart = offset();
    }

    public void addFloat(int slot, float value) {
        if (value != 0.0) {
            prep(INT_BYTES, 0);
            space -= INT_BYTES;
            buffer.putFloat(space, value);
            slots[slot] = offset();
        }
    }

    public void addOffsetField(int slot, int target) {
        if (target != 0) {
            addOffset(target);
            slots[slot] = offset();
        }
    }

    public int endTable() {
        addInt(0);
        int tableEnd = offset();
        int last = slotsInUse - 1;
        while (last >= 0 && slots[last] == 0) {
            last--;
        }
        int keptSlots = last + 1;
        for (int slot = last; slot >= 0; slot--) {
            addShort((short) (slots[slot] != 0 ? tableEnd - slots[slot] : 0));
        }
        addShort((short) (tableEnd - tableStart));
        addShort((short) ((keptSlots + STANDARD_VTABLE_FIELDS) * SHORT_BYTES));
        int existing = matchingVtable();
        if (existing != 0) {
            space = buffer.capacity() - tableEnd;
            buffer.putInt(space, existing - tableEnd);
        } else {
            vtables.add(offset());
            buffer.putInt(buffer.capacity() - tableEnd, offset() - tableEnd);
        }
        return tableEnd;
    }

    private int matchingVtable() {
        for (int remembered : vtables) {
            int first = buffer.capacity() - remembered;
            int second = space;
            short length = buffer.getShort(first);
            if (length != buffer.getShort(second)) {
                continue;
            }
            boolean equal = true;
            for (int position = SHORT_BYTES; position < length; position += SHORT_BYTES) {
                if (buffer.getShort(first + position) != buffer.getShort(second + position)) {
                    equal = false;
                    break;
                }
            }
            if (equal) {
                return remembered;
            }
        }
        return 0;
    }

    public int tableVector(int[] tables) {
        vectorLength = tables.length;
        prep(INT_BYTES, INT_BYTES * tables.length);
        prep(INT_BYTES, INT_BYTES * tables.length);
        for (int index = tables.length - 1; index >= 0; index--) {
            addOffset(tables[index]);
        }
        putInt(vectorLength);
        return offset();
    }

    public byte[] finish(int rootTable) {
        prep(alignment, INT_BYTES);
        addOffset(rootTable);
        byte[] bytes = new byte[buffer.capacity() - space];
        buffer.get(space, bytes);
        return bytes;
    }
}
