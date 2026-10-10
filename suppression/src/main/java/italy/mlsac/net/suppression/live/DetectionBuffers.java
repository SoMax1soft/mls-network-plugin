// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.live;

import italy.mlsac.net.detection.DetectionStates;
import italy.mlsac.net.suppression.suspect.BufferReadout;
import java.util.OptionalDouble;
import java.util.UUID;

public final class DetectionBuffers implements BufferReadout {

    private final DetectionStates detections;

    public DetectionBuffers(DetectionStates detections) {
        this.detections = detections;
    }

    @Override
    public OptionalDouble currentBuffer(UUID suspectId) {
        return detections.buffer(suspectId);
    }
}
