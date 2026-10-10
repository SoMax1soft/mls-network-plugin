// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.util.TimeStampMode;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.Plugin;

public final class PacketEventsRuntime {

    private final PacketEventsAPI<?> api;

    private PacketEventsRuntime(PacketEventsAPI<?> api) {
        this.api = api;
    }

    @SuppressWarnings("deprecation")
    public static PacketEventsRuntime start(Plugin plugin) throws PacketLibraryException {
        try {
            PacketEventsSettings settings = new PacketEventsSettings()
                    .reEncodeByDefault(false)
                    .checkForUpdates(false)
                    .bStats(false)
                    .debug(false)
                    .timeStampMode(TimeStampMode.NANO);
            PacketEventsAPI<?> api = SpigotPacketEventsBuilder.buildNoCache(plugin, settings);
            PacketEvents.setAPI(api);
            if (!api.isLoaded()) {
                api.load();
            }
            if (!api.isInitialized()) {
                api.init();
            }
            return new PacketEventsRuntime(api);
        } catch (RuntimeException | LinkageError failure) {
            throw new PacketLibraryException("packet library refused to start: " + failure, failure);
        }
    }

    public PacketEventsAPI<?> api() {
        return api;
    }

    public void stop() {
        if (!api.isTerminated()) {
            api.terminate();
        }
    }
}
