package wtf.mlsac.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSyncUtilTest {
    @Test
    void copyMissingAddsNestedFieldsInsideExistingSections() throws Exception {
        YamlConfiguration target = new YamlConfiguration();
        target.set("server-identity.name", "box1");
        target.set("server-identity.interserver.enabled", true);

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("server-identity.name", "default");
        defaults.set("server-identity.family", "default");
        defaults.set("server-identity.interserver.enabled", false);
        defaults.set("server-identity.reporting.events-enabled", true);
        defaults.set("server-identity.reporting.alert-threshold", 0.75);

        boolean changed = invokeCopyMissing(target, defaults);

        assertTrue(changed);
        assertEquals("box1", target.getString("server-identity.name"));
        assertEquals(true, target.getBoolean("server-identity.interserver.enabled"));
        assertEquals("default", target.getString("server-identity.family"));
        assertEquals(true, target.getBoolean("server-identity.reporting.events-enabled"));
        assertEquals(0.75, target.getDouble("server-identity.reporting.alert-threshold"));
    }

    @Test
    void copyMissingRepairsWrongSectionTypesSoChildrenCanBeAdded() throws Exception {
        YamlConfiguration target = new YamlConfiguration();
        target.set("messages", "bad-value");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("messages.alert.enabled", true);
        defaults.set("messages.alert.format", "{PLAYER}");

        boolean changed = invokeCopyMissing(target, defaults);

        assertTrue(changed);
        assertTrue(target.isConfigurationSection("messages"));
        assertEquals(true, target.getBoolean("messages.alert.enabled"));
        assertEquals("{PLAYER}", target.getString("messages.alert.format"));
    }

    private boolean invokeCopyMissing(FileConfiguration target, FileConfiguration defaults) throws Exception {
        Method method = ConfigSyncUtil.class.getDeclaredMethod(
                "copyMissing",
                FileConfiguration.class,
                FileConfiguration.class,
                String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, target, defaults, "");
    }
}
