// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend;

@FunctionalInterface
public interface VerdictListener {

    void onVerdict(BackendVerdict verdict);
}
