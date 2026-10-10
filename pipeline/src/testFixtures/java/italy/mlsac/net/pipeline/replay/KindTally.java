// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

final class KindTally {
    long expected;
    long produced;
    long matched;
    long firstMismatch = -1;
    String firstMismatchDetail = "";

    void mismatch(long sequence, String detail) {
        if (firstMismatch < 0) {
            firstMismatch = sequence;
            firstMismatchDetail = detail;
        }
    }
}
