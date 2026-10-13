// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.guard.CommandOrigin;
import italy.mlsac.net.guard.ConsoleGate;
import italy.mlsac.net.guard.ConsoleOrder;

public final class GuardedConsole implements ConsoleCommands {

    private final ConsoleCommands console;
    private final ConsoleGate gate;
    private final CommandOrigin origin;

    public GuardedConsole(ConsoleCommands console, ConsoleGate gate, CommandOrigin origin) {
        this.console = console;
        this.gate = gate;
        this.origin = origin;
    }

    @Override
    public void dispatch(String command) {
        gate.admit(ConsoleOrder.untargeted(command, origin)).ifPresent(console::dispatch);
    }
}
