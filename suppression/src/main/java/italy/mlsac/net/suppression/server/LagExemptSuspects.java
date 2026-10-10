// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server;

import java.util.UUID;

@FunctionalInterface
public interface LagExemptSuspects {

    LagExemptSuspects NONE = suspectId -> false;

    boolean lagExempt(UUID suspectId);
}
