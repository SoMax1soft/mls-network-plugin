// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.restore;

import italy.mlsac.net.detection.RestoreAnswer;
import italy.mlsac.net.detection.RestoreOutcome;
import italy.mlsac.net.detection.RestoreRequest;
import italy.mlsac.net.detection.TickRestore;
import italy.mlsac.net.pipeline.engine.InputTapSource;
import italy.mlsac.net.pipeline.engine.InputTaps;
import italy.mlsac.net.pipeline.engine.PipelinePorts;
import italy.mlsac.net.pipeline.engine.TapParts;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

public final class RestoreDesk implements InputTapSource, TickRestore {
    private final Supplier<RestoreLimits> limits;
    private final RestoreNotes notes;
    private volatile Opened opened;

    public RestoreDesk(Supplier<RestoreLimits> limits, RestoreNotes notes) {
        this.limits = limits;
        this.notes = notes;
    }

    @Override
    public InputTaps open(TapParts parts) {
        PipelinePorts ports = parts.ports();
        RestoreTaps taps = new RestoreTaps(parts, limits, notes);
        opened = new Opened(parts, taps, new RestoreWriter(ports.folder(), ports.platform(), ports.wallClock(),
                ports.uuids(), ports.connections()));
        return taps;
    }

    public Optional<RestoreTaps> taps() {
        Opened current = opened;
        return current == null ? Optional.empty() : Optional.of(current.taps());
    }

    @Override
    public RestoreAnswer restore(RestoreRequest request) {
        Opened current = opened;
        if (current == null || !limits.get().enabled()) {
            return RestoreAnswer.of(RestoreOutcome.DISABLED);
        }
        RestoreBuffer buffer;
        RestoreContent content;
        synchronized (current.parts().turns().of(request.player())) {
            buffer = current.taps().detach(request.player());
            if (buffer == null) {
                return RestoreAnswer.of(RestoreOutcome.NOTHING_BUFFERED);
            }
            content = buffer.finish();
        }
        try {
            if (content.empty()) {
                return RestoreAnswer.of(RestoreOutcome.NOTHING_BUFFERED);
            }
            return current.writer().write(content, request);
        } catch (IOException | RuntimeException failure) {
            notes.trouble("Data restore for " + request.player() + " could not be written: " + failure);
            return RestoreAnswer.of(RestoreOutcome.FAILED);
        } finally {
            current.taps().release(buffer);
        }
    }

    private record Opened(TapParts parts, RestoreTaps taps, RestoreWriter writer) {
    }
}
