// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.response;

import org.bukkit.util.Vector;

public final class ThrowVelocity {

    private ThrowVelocity() {
    }

    public static Vector along(Vector look, double horizontal, double vertical) {
        Vector push = look.clone().multiply(horizontal);
        push.setY(vertical);
        return push;
    }

    public static Vector against(Vector look, double horizontal, double vertical) {
        return along(look, -horizontal, vertical);
    }
}
