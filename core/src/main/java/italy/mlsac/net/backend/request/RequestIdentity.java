// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import italy.mlsac.net.config.core.CoreSettings;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public record RequestIdentity(String buildVersion, String hostLabel, String platformKind, String serverIp,
                              int serverPort, boolean interServer, Optional<String> preset) {

    public static RequestIdentity of(String buildVersion, CoreSettings core, ServerAddress server) {
        var identity = core.identity();
        return new RequestIdentity(buildVersion, identity.name(), identity.family(), server.reportedIp(),
                server.port(), identity.interServer(), core.preset().bodyValue());
    }

    public RequestIdentity {
        Stream.of(buildVersion, hostLabel, platformKind, serverIp, preset).forEach(Objects::requireNonNull);
    }

    public static final int GENERATION_MARK = 2;
}
