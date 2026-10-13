// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import italy.mlsac.net.pipeline.tables.RowFields;
import java.util.Arrays;

final class RowRing {
    private final int capacity;
    private final int chunkRows;
    private final RowChunk[] chunks;
    private final StagedRow staged = new StagedRow();
    private TableShape shape;
    private int head;
    private int size;
    private long dropped;
    private long bytes;

    RowRing(int capacity, int chunkRows) {
        this.capacity = capacity;
        this.chunkRows = chunkRows;
        this.chunks = new RowChunk[(capacity + chunkRows - 1) / chunkRows];
    }

    RowFields stage() {
        staged.reset();
        return staged;
    }

    boolean commit(long stamp, ChunkClaim claim) {
        try {
            return store(stamp, claim);
        } finally {
            staged.reset();
        }
    }

    int size() {
        return size;
    }

    int capacity() {
        return capacity;
    }

    long dropped() {
        return dropped;
    }

    long bytes() {
        return bytes;
    }

    int allocatedChunks() {
        int count = 0;
        for (RowChunk chunk : chunks) {
            if (chunk != null) {
                count++;
            }
        }
        return count;
    }

    long stampAt(int position) {
        int index = indexOf(position);
        return chunks[index / chunkRows].stamp(index % chunkRows);
    }

    void replay(int position, RowFields out) {
        int index = indexOf(position);
        chunks[index / chunkRows].replay(index % chunkRows, shape, out);
    }

    void release() {
        Arrays.fill(chunks, null);
        head = 0;
        size = 0;
        bytes = 0;
    }

    private boolean store(long stamp, ChunkClaim claim) {
        if (shape == null) {
            if (staged.overflow || staged.count == 0) {
                return false;
            }
            shape = TableShape.of(staged);
        }
        if (!shape.fits(staged)) {
            return false;
        }
        int index = size == capacity ? head : (head + size) % capacity;
        int chunkIndex = index / chunkRows;
        RowChunk chunk = chunks[chunkIndex];
        if (chunk == null) {
            int rows = Math.min(chunkRows, capacity - chunkIndex * chunkRows);
            long need = RowChunk.bytesFor(shape, rows);
            if (!claim.claim(need)) {
                return false;
            }
            chunk = new RowChunk(shape, rows);
            chunks[chunkIndex] = chunk;
            bytes += need;
        }
        chunk.store(index % chunkRows, shape, staged, stamp);
        if (size == capacity) {
            head = (head + 1) % capacity;
            dropped++;
        } else {
            size++;
        }
        return true;
    }

    private int indexOf(int position) {
        if (position < 0 || position >= size) {
            throw new IndexOutOfBoundsException(position);
        }
        return (head + position) % capacity;
    }
}
