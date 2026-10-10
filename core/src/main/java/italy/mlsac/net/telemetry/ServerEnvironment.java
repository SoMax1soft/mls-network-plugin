// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.telemetry;

import java.util.Objects;

public record ServerEnvironment(ServerStack stack, HostMachine host) {

    public static final ServerEnvironment UNKNOWN = new ServerEnvironment(ServerStack.UNKNOWN, HostMachine.UNKNOWN);

    public ServerEnvironment {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(host, "host");
    }

    public boolean known() {
        return stack.known() || host.known();
    }
}
