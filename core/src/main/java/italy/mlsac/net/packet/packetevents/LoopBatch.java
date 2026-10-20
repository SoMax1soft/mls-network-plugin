// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import java.util.Arrays;

final class LoopBatch implements Runnable {

    private static final int FIRST_CAPACITY = 16;

    private final PingWrite write;
    private Object[] channels = new Object[FIRST_CAPACITY];
    private int[] ids = new int[FIRST_CAPACITY];
    private int size;

    LoopBatch(PingWrite write) {
        this.write = write;
    }

    void add(Object channel, int id) {
        if (size == ids.length) {
            channels = Arrays.copyOf(channels, size * 2);
            ids = Arrays.copyOf(ids, size * 2);
        }
        channels[size] = channel;
        ids[size] = id;
        size++;
    }

    Object firstChannel() {
        return channels[0];
    }

    int size() {
        return size;
    }

    @Override
    public void run() {
        for (int i = 0; i < size; i++) {
            write.write(channels[i], ids[i]);
        }
    }
}
