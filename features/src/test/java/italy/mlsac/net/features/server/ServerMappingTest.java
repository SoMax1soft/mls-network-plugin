// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.features.server.catalog.NamedConstants;
import italy.mlsac.net.features.server.menu.ClickKinds;
import italy.mlsac.net.features.server.nametag.SpigotTrackingRange;
import italy.mlsac.net.menu.ClickKind;
import italy.mlsac.net.nametag.TrackingRange;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

class ServerMappingTest {

    @Test
    void clicksMapToMenuClickKinds() {
        assertEquals(ClickKind.LEFT, ClickKinds.of(ClickType.LEFT));
        assertEquals(ClickKind.SHIFT_LEFT, ClickKinds.of(ClickType.SHIFT_LEFT));
        assertEquals(ClickKind.RIGHT, ClickKinds.of(ClickType.RIGHT));
        assertEquals(ClickKind.SHIFT_RIGHT, ClickKinds.of(ClickType.SHIFT_RIGHT));
        assertEquals(ClickKind.OTHER, ClickKinds.of(ClickType.NUMBER_KEY));
        assertEquals(ClickKind.OTHER, ClickKinds.of(ClickType.DROP));
        assertEquals(ClickKind.OTHER, ClickKinds.of(null));
    }

    @Test
    void namedConstantsResolveCaseInsensitivelyAndRefuseUnknownNames() {
        NamedConstants<TimeUnit> units = new NamedConstants<>("java.util.concurrent.TimeUnit", TimeUnit.class);
        assertEquals(Optional.of(TimeUnit.SECONDS), units.find(" seconds "));
        assertTrue(units.find("FORTNIGHTS").isEmpty());
        assertTrue(units.find("").isEmpty());
        assertTrue(new NamedConstants<>("org.example.Missing", Object.class).find("ANY").isEmpty());
    }

    @Test
    void theTrackingRangeComesFromSpigotSettings() throws InvalidConfigurationException {
        YamlConfiguration custom = new YamlConfiguration();
        custom.loadFromString("world-settings:\n  default:\n    entity-tracking-range:\n      players: 64\n");
        assertEquals(new TrackingRange(62), SpigotTrackingRange.from(custom));
        YamlConfiguration tiny = new YamlConfiguration();
        tiny.loadFromString("world-settings:\n  default:\n    entity-tracking-range:\n      players: 4\n");
        assertEquals(new TrackingRange(8), SpigotTrackingRange.from(tiny));
        assertEquals(new TrackingRange(46), SpigotTrackingRange.from(new YamlConfiguration()));
        assertEquals(TrackingRange.whenUnreadable(), SpigotTrackingRange.from(null));
    }
}
