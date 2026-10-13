// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection;

@FunctionalInterface
public interface TickRestore {

    TickRestore NONE = request -> RestoreAnswer.of(RestoreOutcome.DISABLED);

    RestoreAnswer restore(RestoreRequest request);
}
