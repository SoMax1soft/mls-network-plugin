// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player.bukkit;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import italy.mlsac.net.player.PermissionChanges;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class LuckPermsChanges implements PermissionChanges {

    private static final String PLUGIN = "LuckPerms";
    private static final String PROVIDER = "net.luckperms.api.LuckPermsProvider";
    private static final String EVENT = "net.luckperms.api.event.user.UserDataRecalculateEvent";
    private static final String USER = "net.luckperms.api.model.user.User";

    private final Plugin owner;
    private final PluginLog log;

    private LuckPermsChanges(Plugin owner, PluginLog log) {
        this.owner = owner;
        this.log = log;
    }

    public static PermissionChanges detect(Plugin owner, PluginLog log) {
        Plugin luckPerms = Bukkit.getPluginManager().getPlugin(PLUGIN);
        return luckPerms == null ? NONE : new LuckPermsChanges(owner, log);
    }

    @Override
    public Closing listen(Consumer<UUID> changed) {
        try {
            Plugin luckPerms = Bukkit.getPluginManager().getPlugin(PLUGIN);
            if (luckPerms == null || !luckPerms.isEnabled()) {
                return NONE.listen(changed);
            }
            ClassLoader loader = luckPerms.getClass().getClassLoader();
            Object api = Class.forName(PROVIDER, true, loader).getMethod("get").invoke(null);
            Object bus = api.getClass().getMethod("getEventBus").invoke(api);
            Class<?> event = Class.forName(EVENT, true, loader);
            Method user = event.getMethod("getUser");
            Method uniqueId = Class.forName(USER, true, loader).getMethod("getUniqueId");
            Consumer<Object> handler = fired -> {
                try {
                    changed.accept((UUID) uniqueId.invoke(user.invoke(fired)));
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    log.debug(LogTopic.GENERAL, () -> "A LuckPerms change could not be read: " + failure);
                }
            };
            Method subscribe = Class.forName("net.luckperms.api.event.EventBus", true, loader)
                    .getMethod("subscribe", Object.class, Class.class, Consumer.class);
            Object subscription = subscribe.invoke(bus, owner, event, handler);
            Method close = Class.forName("net.luckperms.api.event.EventSubscription", true, loader).getMethod("close");
            return () -> {
                try {
                    close.invoke(subscription);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    log.debug(LogTopic.GENERAL, () -> "LuckPerms listener already gone");
                }
            };
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unsupported) {
            log.debug(LogTopic.GENERAL, () -> "LuckPerms changes are not observed: " + unsupported);
            return NONE.listen(changed);
        }
    }
}
