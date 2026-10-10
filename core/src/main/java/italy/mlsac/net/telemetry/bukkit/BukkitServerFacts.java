// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.telemetry.bukkit;

import italy.mlsac.net.platform.PlatformProfile;
import italy.mlsac.net.telemetry.HostMachine;
import italy.mlsac.net.telemetry.ServerEnvironment;
import italy.mlsac.net.telemetry.ServerStack;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.Bukkit;

public final class BukkitServerFacts {

    private BukkitServerFacts() {
    }

    public static ServerEnvironment read(PlatformProfile platform) {
        return new ServerEnvironment(stack(platform), HostMachine.read());
    }

    private static ServerStack stack(PlatformProfile platform) {
        return ServerStack.of(platform, text(Bukkit::getName), text(Bukkit::getVersion),
                text(Bukkit::getBukkitVersion));
    }

    private static Optional<String> text(Supplier<String> source) {
        try {
            return Optional.ofNullable(source.get());
        } catch (RuntimeException | LinkageError unavailable) {
            return Optional.empty();
        }
    }
}
