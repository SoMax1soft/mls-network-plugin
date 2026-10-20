// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.detection.choice;

import italy.mlsac.net.api.FallbackReason;
import italy.mlsac.net.api.ModelChoice;

@FunctionalInterface
public interface ModelRefusals {

    void refused(ModelChoice model, FallbackReason reason);
}
