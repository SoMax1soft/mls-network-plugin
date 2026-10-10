// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.packet;

import italy.mlsac.net.platform.ClientVersionRules;
import italy.mlsac.net.platform.GameVersion;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public interface ClientVersions {

    ClientVersions NONE = new ClientVersions() {

        @Override
        public Optional<GameVersion> version(UUID player) {
            return Optional.empty();
        }

        @Override
        public Optional<String> versionName(UUID player) {
            return Optional.empty();
        }

        @Override
        public OptionalInt protocol(UUID player) {
            return OptionalInt.empty();
        }
    };

    Optional<GameVersion> version(UUID player);

    Optional<String> versionName(UUID player);

    OptionalInt protocol(UUID player);

    default ClientVersionRules rules(UUID player) {
        return new ClientVersionRules(version(player));
    }
}
