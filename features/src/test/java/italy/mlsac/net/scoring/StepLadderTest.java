// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StepLadderTest {
    private static final double BUFFER = 40;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "0.33 | 330 660 990 | 0.33 0.66 0.99 | 13.200000000000001 26.400000000000002 39.6 | 33 66 99",
        "0.25 | 250 500 750 1000 | 0.25 0.5 0.75 1.0 | 10.0 20.0 30.0 40.0 | 25 50 75 100",
        "0.3 | 300 600 900 | 0.3 0.6 0.8999999999999999 | 12.0 24.0 36.0 | 30 60 90",
        "1.0 | 1000 | 1.0 | 40.0 | 100",
        "1.5 | 330 660 990 | 0.33 0.66 0.99 | 13.200000000000001 26.400000000000002 39.6 | 33 66 99",
        "0.0 | 330 660 990 | 0.33 0.66 0.99 | 13.200000000000001 26.400000000000002 39.6 | 33 66 99"
    })
    void ladderShape(double step, String keys, String fractions, String thresholds, String percents) {
        List<AlertStep> ladder = StepLadder.build(step);
        assertEquals(longs(keys), ladder.stream().map(AlertStep::key).toList());
        assertEquals(doubles(fractions), ladder.stream().map(AlertStep::fraction).toList());
        assertEquals(doubles(thresholds), ladder.stream().map(rung -> rung.threshold(BUFFER)).toList());
        assertEquals(longs(percents), ladder.stream().map(AlertStep::percent).toList());
    }

    @ParameterizedTest
    @CsvSource({"0.1, 10, 0.9999999999999999, 1000, alert-step-1000"})
    void lastRungOfTenPercent(double step, int size, double fraction, long key, String latch) {
        List<AlertStep> ladder = StepLadder.build(step);
        AlertStep top = ladder.get(ladder.size() - 1);
        assertEquals(size, ladder.size());
        assertEquals(fraction, top.fraction());
        assertEquals(key, top.key());
        assertEquals(latch, top.latchName());
    }

    private static List<Long> longs(String spaced) {
        return Arrays.stream(spaced.trim().split(" +")).map(Long::valueOf).toList();
    }

    private static List<Double> doubles(String spaced) {
        return Arrays.stream(spaced.trim().split(" +")).map(Double::valueOf).toList();
    }
}
