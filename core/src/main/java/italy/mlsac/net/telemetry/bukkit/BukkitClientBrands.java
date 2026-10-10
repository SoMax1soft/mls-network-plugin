// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.telemetry.bukkit;

import italy.mlsac.net.telemetry.ClientBrands;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BukkitClientBrands implements ClientBrands {

    static final String BRAND_METHOD = "getClientBrandName";

    private final Method brandMethod;

    private BukkitClientBrands(Method brandMethod) {
        this.brandMethod = brandMethod;
    }

    public static ClientBrands detect() {
        try {
            Method method = Player.class.getMethod(BRAND_METHOD);
            return String.class.equals(method.getReturnType()) ? new BukkitClientBrands(method) : ClientBrands.NONE;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            return ClientBrands.NONE;
        }
    }

    @Override
    public Optional<String> brand(UUID player) {
        Player online = Bukkit.getPlayer(player);
        if (online == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable((String) brandMethod.invoke(online));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unsupported) {
            return Optional.empty();
        }
    }
}
