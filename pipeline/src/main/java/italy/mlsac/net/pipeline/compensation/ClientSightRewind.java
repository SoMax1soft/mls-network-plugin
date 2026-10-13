// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.compensation;

import italy.mlsac.net.pipeline.geometry.Box;
import italy.mlsac.net.pipeline.geometry.BoxSpan;
import italy.mlsac.net.pipeline.geometry.HitMeasurement;
import italy.mlsac.net.pipeline.geometry.HitMeasurer;
import italy.mlsac.net.pipeline.geometry.LookPair;
import italy.mlsac.net.pipeline.player.PlayerSample;
import italy.mlsac.net.pipeline.player.PositionHistory;
import italy.mlsac.net.pipeline.player.ServerSamples;
import java.util.Optional;
import java.util.UUID;

import java.util.concurrent.TimeUnit;

public final class ClientSightRewind {
    private static final long TRAIL_NANOS = TimeUnit.MILLISECONDS.toNanos(150);

    private final CompensationBook book;
    private final ServerSamples samples;

    public ClientSightRewind(CompensationBook book, ServerSamples samples) {
        this.book = book;
        this.samples = samples;
    }

    public TargetMeasurement measure(UUID viewer, PlayerSample eye, PlayerSample target, long now, LookPair look) {
        LensReading lens = read(viewer, eye, target, now);
        if (lens.mode() == LensMode.NONE) {
            return new TargetMeasurement(lens, HitMeasurement.INVALID);
        }
        HitMeasurement hit = HitMeasurer.measure(eye.feet(), eye.eyeLift(), look, lens.possible(), lens.tight());
        return new TargetMeasurement(lens, hit);
    }

    private LensReading read(UUID viewer, PlayerSample eye, PlayerSample target, long now) {
        CompensatedViewer tracked = book.viewerOrNull(viewer);
        if (tracked != null && !riding(target)) {
            LensReading acknowledged = acknowledged(tracked, target);
            if (acknowledged != null) {
                return acknowledged;
            }
        }
        return rewound(viewer, tracked, eye, target, now);
    }

    private LensReading acknowledged(CompensatedViewer viewer, PlayerSample target) {
        PingAcknowledgements acknowledgements = viewer.acknowledgements();
        if (!acknowledgements.backlogSmall() || acknowledgements.roundTripMillis() < 0) {
            return null;
        }
        AcknowledgedBody entity = viewer.entityOrNull(target.entityNumber());
        if (entity == null) {
            return null;
        }
        return new LensReading(LensMode.TRANSACTIONS, acknowledgements.roundTripMillis(),
                acknowledgements.probesUnanswered(), entity.possibleBox(target.width(), target.height()),
                entity.tightBox(target.width(), target.height()));
    }

    private LensReading rewound(UUID viewer, CompensatedViewer tracked, PlayerSample eye,
            PlayerSample target, long now) {
        double ping = rewindPing(viewer, tracked);
        if (ping < 0) {
            return LensReading.NONE;
        }
        PositionHistory history = samples.history(target.player());
        if (history == null) {
            return LensReading.NONE;
        }
        long seen = now - (long) (ping * 1_000_000);
        Optional<PlayerSample> at = history.at(seen);
        Optional<PlayerSample> from = history.at(seen - TRAIL_NANOS);
        if (at.isEmpty() || from.isEmpty() || !at.get().world().equals(eye.world())
                || at.get().player().equals(viewer)) {
            return LensReading.NONE;
        }
        PlayerSample anchor = at.get();
        Box tight = boxOf(anchor);
        BoxSpan span = new BoxSpan(tight);
        PlayerSample earliest = from.get();
        if (PositionHistory.oneUnbrokenRun(earliest, anchor)) {
            span.includePlayer(earliest.x(), earliest.y(), earliest.z(), earliest.width(), earliest.height());
        }
        history.spanRun(seen - TRAIL_NANOS, seen, anchor, span);
        Box possible = span.toBox();
        int outstanding = tracked == null ? -1 : tracked.acknowledgements().probesUnanswered();
        return new LensReading(LensMode.REWIND, ping, outstanding, possible, tight);
    }

    private double rewindPing(UUID viewer, CompensatedViewer tracked) {
        if (tracked != null && tracked.acknowledgements().roundTripMillis() >= 0) {
            return tracked.acknowledgements().roundTripMillis();
        }
        return samples.serverPing(viewer, -1);
    }

    private static boolean riding(PlayerSample target) {
        return target.posture() != null && target.posture().riding();
    }

    private static Box boxOf(PlayerSample sample) {
        return Box.player(sample.x(), sample.y(), sample.z(), sample.width(), sample.height());
    }
}
