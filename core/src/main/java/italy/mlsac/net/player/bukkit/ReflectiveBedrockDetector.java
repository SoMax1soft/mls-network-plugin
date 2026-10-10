// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player.bukkit;

import italy.mlsac.net.player.BedrockDetector;
import java.lang.reflect.Method;
import java.util.UUID;

public final class ReflectiveBedrockDetector implements BedrockDetector {

    private final Object floodgate;
    private final Method floodgateCheck;
    private final Object geyser;
    private final Method geyserCheck;

    private ReflectiveBedrockDetector(Object floodgate, Method floodgateCheck, Object geyser, Method geyserCheck) {
        this.floodgate = floodgate;
        this.floodgateCheck = floodgateCheck;
        this.geyser = geyser;
        this.geyserCheck = geyserCheck;
    }

    public static BedrockDetector detect(ClassLoader loader) {
        Object floodgate = null;
        Method floodgateCheck = null;
        Object geyser = null;
        Method geyserCheck = null;
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, loader);
            floodgate = api.getMethod("getInstance").invoke(null);
            floodgateCheck = api.getMethod("isFloodgatePlayer", UUID.class);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            floodgate = null;
        }
        try {
            Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, loader);
            geyser = api.getMethod("api").invoke(null);
            geyserCheck = api.getMethod("isBedrockPlayer", UUID.class);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            geyser = null;
        }
        if (floodgate == null && geyser == null) {
            return BedrockDetector.NONE;
        }
        return new ReflectiveBedrockDetector(floodgate, floodgateCheck, geyser, geyserCheck);
    }

    @Override
    public boolean bedrockClient(UUID player) {
        return check(floodgate, floodgateCheck, player) || check(geyser, geyserCheck, player);
    }

    private static boolean check(Object api, Method method, UUID player) {
        if (api == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(api, player));
        } catch (ReflectiveOperationException | RuntimeException failed) {
            return false;
        }
    }
}
