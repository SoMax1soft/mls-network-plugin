// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.bukkit.Server;

final class SharedSightCheck {

    static final String PROPERTY = "mlsac.sight.shared";
    static final String LEAF_PARALLEL_TICKING = "org.dreeam.leaf.config.modules.async.SparklyPaperParallelWorldTicking";
    private static final Set<String> VERIFIED_SERVERS = Set.of("paper", "leaf");
    private static final String VERIFIED_RELEASE = "1.21.4-";
    private static final BooleanSupplier ON = () -> true;
    private static final BooleanSupplier OFF = () -> false;

    private SharedSightCheck() {
    }

    static boolean enabled(Server server) {
        return switchFor(server).getAsBoolean();
    }

    static BooleanSupplier switchFor(Server server) {
        String forced = System.getProperty(PROPERTY, "").trim().toLowerCase(Locale.ROOT);
        if (forced.equals("on")) {
            return ON;
        }
        if (forced.equals("off") || server == null) {
            return OFF;
        }
        String name = server.getName();
        if (name == null || !VERIFIED_SERVERS.contains(name.trim().toLowerCase(Locale.ROOT))
                || !server.getBukkitVersion().startsWith(VERIFIED_RELEASE)) {
            return OFF;
        }
        return unlessParallel(server.getClass().getClassLoader(), LEAF_PARALLEL_TICKING);
    }

    static BooleanSupplier unlessParallel(ClassLoader loader, String className) {
        Field flag = parallelFlag(loader, className);
        if (flag == null) {
            return ON;
        }
        return () -> {
            try {
                return !flag.getBoolean(null);
            } catch (IllegalAccessException | RuntimeException unreadable) {
                return false;
            }
        };
    }

    private static Field parallelFlag(ClassLoader loader, String className) {
        try {
            Field flag = Class.forName(className, false, loader).getField("enabled");
            boolean usable = Modifier.isStatic(flag.getModifiers()) && flag.getType() == boolean.class;
            return usable ? flag : null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            return null;
        }
    }
}
