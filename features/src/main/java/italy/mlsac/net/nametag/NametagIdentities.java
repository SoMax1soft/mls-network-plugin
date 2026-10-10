// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.nametag;

import java.util.concurrent.atomic.AtomicInteger;

public final class NametagIdentities {

    static final int FIRST_ID = Integer.MAX_VALUE - 1;

    private final AtomicInteger next = new AtomicInteger(FIRST_ID);

    public NametagIdentity allocate() {
        return NametagIdentity.of(next.getAndDecrement());
    }
}
