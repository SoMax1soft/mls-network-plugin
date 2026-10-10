// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.workload;

import java.util.LinkedHashMap;
import java.util.Map;

public record CounterReading(long hologramNanos, long hologramPackets, long hologramTextBuilds, long pipelineNanos,
                             long pipelineEvents, long payloadNanos, long payloadBuilds, long payloadBytes,
                             long responseNanos, long responses, long virtualStepAlerts, long virtualFlags,
                             long virtualSuppressedPunishments) {

    public static final CounterReading ZERO = new CounterReading(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    public CounterReading minus(CounterReading earlier) {
        return new CounterReading(hologramNanos - earlier.hologramNanos, hologramPackets - earlier.hologramPackets,
                hologramTextBuilds - earlier.hologramTextBuilds, pipelineNanos - earlier.pipelineNanos,
                pipelineEvents - earlier.pipelineEvents, payloadNanos - earlier.payloadNanos,
                payloadBuilds - earlier.payloadBuilds, payloadBytes - earlier.payloadBytes,
                responseNanos - earlier.responseNanos, responses - earlier.responses,
                virtualStepAlerts - earlier.virtualStepAlerts, virtualFlags - earlier.virtualFlags,
                virtualSuppressedPunishments - earlier.virtualSuppressedPunishments);
    }

    public Map<String, Long> named() {
        Map<String, Long> values = new LinkedHashMap<>();
        values.put("hologram_nanos", hologramNanos);
        values.put("hologram_packets", hologramPackets);
        values.put("hologram_text_builds", hologramTextBuilds);
        values.put("pipeline_nanos", pipelineNanos);
        values.put("pipeline_events", pipelineEvents);
        values.put("payload_nanos", payloadNanos);
        values.put("payload_builds", payloadBuilds);
        values.put("payload_bytes", payloadBytes);
        values.put("response_nanos", responseNanos);
        values.put("responses", responses);
        values.put("virtual_step_alerts", virtualStepAlerts);
        values.put("virtual_flags", virtualFlags);
        values.put("virtual_suppressed_punishments", virtualSuppressedPunishments);
        return values;
    }
}
