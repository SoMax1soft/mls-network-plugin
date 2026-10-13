// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import italy.mlsac.net.pipeline.engine.InputTap;
import italy.mlsac.net.pipeline.engine.InputTaps;
import italy.mlsac.net.pipeline.engine.TapParts;
import italy.mlsac.net.pipeline.environment.UuidPurpose;
import italy.mlsac.net.pipeline.environment.WallPurpose;
import italy.mlsac.net.pipeline.input.AttackPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class RestoreTaps implements InputTaps, BufferBudget {
    private final TapParts parts;
    private final Supplier<RestoreLimits> limits;
    private final RestoreNotes notes;
    private final ConcurrentHashMap<UUID, RestoreBuffer> buffers = new ConcurrentHashMap<>();
    private final InputTap admission = new AdmissionTap(this::admit);
    private final Object ledger = new Object();
    private long usedBytes;
    private volatile boolean closed;

    RestoreTaps(TapParts parts, Supplier<RestoreLimits> limits, RestoreNotes notes) {
        this.parts = parts;
        this.limits = limits;
        this.notes = notes;
    }

    @Override
    public InputTap tapOrNull(UUID player) {
        if (closed || !limits.get().enabled()) {
            if (!buffers.isEmpty()) {
                retireAll("data restore switched off");
            }
            return null;
        }
        RestoreBuffer buffer = buffers.get(player);
        return buffer != null ? buffer : admission;
    }

    @Override
    public boolean anyActive() {
        return !closed && limits.get().enabled();
    }

    @Override
    public void closeAll() {
        closed = true;
        retireAll("plugin stopped");
    }

    @Override
    public void playerLeft(UUID player) {
        synchronized (ledger) {
            RestoreBuffer buffer = buffers.get(player);
            if (buffer != null) {
                retire(buffer, "player left");
            }
        }
    }

    @Override
    public boolean claim(RestoreBuffer owner, long bytes) {
        synchronized (ledger) {
            if (owner.retired()) {
                return false;
            }
            long cap = limits.get().maxBytes();
            while (usedBytes + bytes > cap) {
                RestoreBuffer idle = longestIdle(owner);
                if (idle == null) {
                    notes.note(() -> "data restore: memory cap reached, " + owner.player() + " keeps its current rows");
                    return false;
                }
                retire(idle, "memory cap");
            }
            usedBytes += bytes;
            owner.addClaimed(bytes);
            long total = usedBytes;
            notes.note(() -> "data restore: " + owner.player() + " holds " + owner.claimedBytes() + " bytes ("
                    + owner.keptTicks() + " ticks, " + owner.keptHits() + " hits), all players " + total + " of "
                    + cap + " bytes");
            return true;
        }
    }

    public int bufferedPlayers() {
        return buffers.size();
    }

    public long usedBytes() {
        synchronized (ledger) {
            return usedBytes;
        }
    }

    public boolean buffering(UUID player) {
        return buffers.containsKey(player);
    }

    RestoreBuffer detach(UUID player) {
        synchronized (ledger) {
            return buffers.remove(player);
        }
    }

    void release(RestoreBuffer buffer) {
        synchronized (ledger) {
            buffer.retire();
            usedBytes -= buffer.takeClaimed();
        }
        buffer.release();
    }

    private void admit(AttackPacket packet) {
        if (parts.samples().board().byEntity(packet.entityNumber()) == null) {
            return;
        }
        UUID player = packet.player();
        RestoreLimits now = limits.get();
        RestoreBuffer buffer = new RestoreBuffer(player, parts.ports().uuids().next(UuidPurpose.RESTORE_ID),
                parts.ports().wallClock().instant(WallPurpose.RESTORE_START), packet.nanos(), parts.samples(),
                parts.lens(), parts.probe(), this);
        synchronized (ledger) {
            while (buffers.size() >= now.maxPlayers()) {
                RestoreBuffer idle = longestIdle(null);
                if (idle == null) {
                    return;
                }
                retire(idle, "player cap");
            }
            buffers.put(player, buffer);
        }
        notes.note(() -> "data restore: buffering " + player);
        buffer.attack(packet);
    }

    private RestoreBuffer longestIdle(RestoreBuffer keep) {
        RestoreBuffer idle = null;
        for (RestoreBuffer buffer : buffers.values()) {
            if (buffer != keep && (idle == null || buffer.lastFightNanos() < idle.lastFightNanos())) {
                idle = buffer;
            }
        }
        return idle;
    }

    private void retire(RestoreBuffer buffer, String reason) {
        buffers.remove(buffer.player(), buffer);
        buffer.retire();
        long freed = buffer.takeClaimed();
        usedBytes -= freed;
        notes.note(() -> "data restore: dropped " + buffer.player() + " (" + reason + "), freed " + freed + " bytes");
    }

    private void retireAll(String reason) {
        synchronized (ledger) {
            List<RestoreBuffer> all = new ArrayList<>(buffers.values());
            for (RestoreBuffer buffer : all) {
                retire(buffer, reason);
            }
        }
    }
}
