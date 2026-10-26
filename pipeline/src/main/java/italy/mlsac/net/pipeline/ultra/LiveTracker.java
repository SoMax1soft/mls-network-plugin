// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import italy.mlsac.net.console.DebugTally;
import italy.mlsac.net.pipeline.compensation.ClientSightRewind;
import italy.mlsac.net.pipeline.environment.UltraWindowSink;
import italy.mlsac.net.pipeline.occlusion.LiveOcclusion;
import italy.mlsac.net.pipeline.occlusion.OcclusionProbe;
import italy.mlsac.net.pipeline.occlusion.OcclusionQueue;
import italy.mlsac.net.pipeline.player.ServerSamples;
import italy.mlsac.net.pipeline.rows.NoPacketRowInput;
import italy.mlsac.net.pipeline.rows.RowProducer;
import java.util.UUID;
import java.util.function.IntSupplier;

public final class LiveTracker {
    private final RowProducer recorder;

    public LiveTracker(UUID player, long start, ServerSamples samples, ClientSightRewind lens, OcclusionProbe probe,
            UltraWindowSink sink) {
        this(player, start, samples, lens, probe, sink, () -> 20);
    }

    public LiveTracker(UUID player, long start, ServerSamples samples, ClientSightRewind lens, OcclusionProbe probe,
            UltraWindowSink sink, IntSupplier wallChecksPerSecond) {
        FightAssembler assembler = new FightAssembler(
                window -> {
                    DebugTally.count("ultra.window");
                    DebugTally.trace(player, () -> "Ultra window ready");
                    sink.encodeAndAccept(player, () -> MlsuWire.encode(WireVersion.V6, window));
                });
        this.recorder = new RowProducer(player, start, assembler, samples, lens,
                new OcclusionQueue(new LiveOcclusion(probe, wallChecksPerSecond)),
                NoPacketRowInput.CARRIED_TO_NEXT_PACKET);
    }

    public RowProducer rows() {
        return recorder;
    }
}
