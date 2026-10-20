// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

final class BurstPings implements PingDelivery {

    private final ReachableChannels channels;
    private final PingWrite write;
    private final PingDelivery single;
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private final Map<Object, LoopBatch> batches = new IdentityHashMap<>();

    BurstPings(ReachableChannels channels, PingWrite write) {
        this.channels = channels;
        this.write = write;
        this.single = new LoopPings(channels, write);
    }

    @Override
    public void deliver(Object channel, int id) {
        if (owner.get() != Thread.currentThread()) {
            single.deliver(channel, id);
            return;
        }
        Object loop = channels.loopOrNull(channel);
        if (loop == null) {
            single.deliver(channel, id);
            return;
        }
        LoopBatch batch = batches.get(loop);
        if (batch == null) {
            batch = new LoopBatch(write);
            batches.put(loop, batch);
        }
        batch.add(channel, id);
    }

    @Override
    public void openBurst() {
        owner.compareAndSet(null, Thread.currentThread());
    }

    @Override
    public void closeBurst() {
        if (owner.get() != Thread.currentThread()) {
            return;
        }
        try {
            for (LoopBatch batch : batches.values()) {
                channels.submit(batch.firstChannel(), "handing transaction pings to a network thread", batch);
            }
        } finally {
            batches.clear();
            owner.set(null);
        }
    }
}
