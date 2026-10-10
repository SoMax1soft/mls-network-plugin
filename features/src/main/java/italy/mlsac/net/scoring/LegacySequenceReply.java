// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import italy.mlsac.net.logbook.Logbook;

public final class LegacySequenceReply {

    static final int MIN_LENGTH = 5;
    static final int MAX_LENGTH = 200;

    private final LegacyWindowLength windowLength;
    private final Logbook log;

    public LegacySequenceReply(LegacyWindowLength windowLength, Logbook log) {
        this.windowLength = windowLength;
        this.log = log;
    }

    public void handle(String error) {
        String[] parts = error.split(":");
        if (parts.length < 2) {
            return;
        }
        int requested;
        try {
            requested = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException unparsable) {
            log.warn("[Detection] Window length in backend error is not a number: " + error);
            return;
        }
        if (requested < MIN_LENGTH || requested > MAX_LENGTH) {
            log.warn("[Detection] Backend asked for window length " + requested + ", kept the old one (5..200 allowed)");
            return;
        }
        if (requested == windowLength.current()) {
            return;
        }
        windowLength.change(requested);
        log.info("[Detection] Legacy window length changed to " + requested + " by the backend");
    }
}
