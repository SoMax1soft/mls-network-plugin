// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

public record RunningSession(BackendSession session, AutoCloseable resources) {
}
