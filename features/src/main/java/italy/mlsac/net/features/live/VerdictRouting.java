// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.live;

import italy.mlsac.net.detection.ModelVerdict;
import italy.mlsac.net.detection.WindowKind;
import italy.mlsac.net.detection.WindowSubmitted;
import italy.mlsac.net.scoring.AnswerIntake;
import italy.mlsac.net.scoring.DetectionFlow;
import italy.mlsac.net.scoring.ModelAnswer;
import italy.mlsac.net.scoring.SuspectRecord;
import italy.mlsac.net.scoring.SuspectRoster;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class VerdictRouting {

    private static final int REMEMBERED_WINDOWS = 4096;

    private final SuspectRoster roster;
    private final DetectionFlow flow;
    private final AnswerIntake ultra;
    private final AnswerIntake legacy;
    private final Map<Long, SuspectRecord> legacyOwners = new LinkedHashMap<>(256, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, SuspectRecord> eldest) {
            return size() > REMEMBERED_WINDOWS;
        }
    };

    public VerdictRouting(SuspectRoster roster, DetectionFlow flow, AnswerIntake ultra, AnswerIntake legacy) {
        this.roster = roster;
        this.flow = flow;
        this.ultra = ultra;
        this.legacy = legacy;
    }

    public void submitted(WindowSubmitted window) {
        if (window.kind() != WindowKind.LEGACY) {
            return;
        }
        roster.active(window.player()).ifPresent(record -> remember(window.ticket(), record));
    }

    public void answered(ModelVerdict verdict) {
        ModelAnswer answer = ModelAnswer.of(verdict.probability(), verdict.model());
        if (verdict.kind() == WindowKind.ULTRA) {
            roster.active(verdict.player()).ifPresent(record ->
                    ultra.admit(answer).ifPresent(admitted -> flow.process(record, admitted)));
            return;
        }
        owner(verdict.ticket()).ifPresent(record ->
                legacy.admit(answer).ifPresent(admitted -> flow.process(record, admitted)));
    }

    public synchronized void clear() {
        legacyOwners.clear();
    }

    private synchronized void remember(long ticket, SuspectRecord record) {
        legacyOwners.put(ticket, record);
    }

    private synchronized Optional<SuspectRecord> owner(long ticket) {
        return Optional.ofNullable(legacyOwners.get(ticket));
    }
}
