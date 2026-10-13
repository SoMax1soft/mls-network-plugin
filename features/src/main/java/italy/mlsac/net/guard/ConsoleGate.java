// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Optional;

public interface ConsoleGate {

    ConsoleGate OPEN = order -> Optional.of(order.command());

    Optional<String> admit(ConsoleOrder order);
}
