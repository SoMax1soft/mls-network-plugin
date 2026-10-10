// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

public record ConfigIssue(LayerSource source, String path, String reason, IssueSeverity severity) {
}
