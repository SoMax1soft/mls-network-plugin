// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.telemetry;

import italy.mlsac.net.platform.PlatformProfile;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public record ServerStack(Optional<String> name, Optional<String> version, Optional<String> apiVersion,
                          Optional<String> software, Optional<String> gameVersion, Optional<String> compatibility,
                          Optional<Boolean> folia) {

    public static final ServerStack UNKNOWN = new ServerStack(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    private static final int TEXT_LIMIT = 128;

    public ServerStack {
        Stream.of(name, version, apiVersion, software, gameVersion, compatibility, folia)
                .forEach(Objects::requireNonNull);
        name = trimmed(name);
        version = trimmed(version);
        apiVersion = trimmed(apiVersion);
        software = trimmed(software);
        gameVersion = trimmed(gameVersion);
        compatibility = trimmed(compatibility);
    }

    public static ServerStack of(PlatformProfile platform, Optional<String> name, Optional<String> version,
                                 Optional<String> apiVersion) {
        if (platform == null) {
            return new ServerStack(name, version, apiVersion, Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty());
        }
        return new ServerStack(name, version, apiVersion,
                Optional.of(platform.software().displayName()),
                Optional.of(platform.serverVersion().version().toString()),
                Optional.of(platform.serverVersion().mode().name()),
                Optional.of(platform.folia()));
    }

    public boolean known() {
        return name.isPresent() || version.isPresent() || apiVersion.isPresent() || software.isPresent()
                || gameVersion.isPresent() || compatibility.isPresent() || folia.isPresent();
    }

    private static Optional<String> trimmed(Optional<String> value) {
        return value.map(String::trim).filter(text -> !text.isEmpty())
                .map(text -> text.length() <= TEXT_LIMIT ? text : text.substring(0, TEXT_LIMIT));
    }
}
