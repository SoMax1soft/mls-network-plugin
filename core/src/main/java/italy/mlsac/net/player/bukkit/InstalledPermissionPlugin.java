// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.player.bukkit;

import italy.mlsac.net.player.PermissionAuthority;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class InstalledPermissionPlugin implements PermissionAuthority {

    private static final List<String> KNOWN = List.of("LuckPerms", "PermissionsEx", "GroupManager", "UltraPermissions",
            "PowerRanks", "zPermissions", "bPermissions", "PermissionsBukkit");
    private static final String VAULT_PERMISSION = "net.milkbowl.vault.permission.Permission";
    private static final String VAULT_FALLBACK = "SuperPerms";

    @Override
    public boolean installed() {
        PluginManager plugins = Bukkit.getPluginManager();
        for (String name : KNOWN) {
            Plugin plugin = plugins.getPlugin(name);
            if (plugin != null && plugin.isEnabled()) {
                return true;
            }
        }
        return vaultProvider();
    }

    private static boolean vaultProvider() {
        try {
            Class<?> service = Class.forName(VAULT_PERMISSION);
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(service);
            if (registration == null) {
                return false;
            }
            Object provider = registration.getProvider();
            Object name = provider.getClass().getMethod("getName").invoke(provider);
            return name != null && !VAULT_FALLBACK.equalsIgnoreCase(name.toString());
        } catch (ReflectiveOperationException | LinkageError absent) {
            return false;
        }
    }
}
