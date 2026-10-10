// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

final class ContainedTask implements Runnable {

    private final Runnable task;
    private final PacketFaultLog faults;

    ContainedTask(Runnable task, PacketFaultLog faults) {
        this.task = task;
        this.faults = faults;
    }

    @Override
    public void run() {
        try {
            task.run();
        } catch (RuntimeException | Error failure) {
            faults.recordNetworkFault("a task on a player's network thread", failure);
        }
    }
}
