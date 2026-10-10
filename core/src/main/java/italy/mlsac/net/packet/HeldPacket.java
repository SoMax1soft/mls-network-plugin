// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HeldPacket {

    private final UUID player;
    private final Object payload;
    private final AtomicBoolean settled = new AtomicBoolean();

    public HeldPacket(UUID player, Object payload) {
        this.player = Objects.requireNonNull(player, "player");
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    public UUID player() {
        return player;
    }

    public Object payload() {
        return payload;
    }

    public boolean settle() {
        return settled.compareAndSet(false, true);
    }

    public boolean settled() {
        return settled.get();
    }
}
