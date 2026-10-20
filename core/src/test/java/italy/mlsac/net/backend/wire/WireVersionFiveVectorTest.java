// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.json.JsonArray;
import italy.mlsac.net.json.JsonNumber;
import italy.mlsac.net.json.JsonObject;
import italy.mlsac.net.json.JsonString;
import italy.mlsac.net.json.JsonValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WireVersionFiveVectorTest {

    @Test
    void generatorsMatchTheVectors() throws Exception {
        for (JsonObject set : WireVectors.sets(WireVectors.load())) {
            WireSeed seed = seedOf(set);
            SplitMix64 order = new SplitMix64(seed.bits());
            SplitMix64 jitter = new SplitMix64(seed.jitterBits());
            assertEquals(texts(set.array("splitmix_first_outputs").orElseThrow()),
                    List.of(next(order), next(order), next(order), next(order)), seed.toString());
            assertEquals(texts(set.array("noise_splitmix_first_outputs").orElseThrow()),
                    List.of(next(jitter), next(jitter), next(jitter), next(jitter)), seed.toString());
            SlotOrder slots = SlotOrder.shuffled(seed, ScrambledWindows.SLOTS);
            List<JsonValue> expected = set.array("P").orElseThrow().elements();
            assertEquals(ScrambledWindows.SLOTS, expected.size());
            for (int slot = 0; slot < ScrambledWindows.SLOTS; slot++) {
                assertEquals(((JsonNumber) expected.get(slot)).doubleValue(), slots.sourceOf(slot), "P[" + slot + "]");
            }
        }
    }

    @Test
    void jitterAndDerivedColumnsOfTheFirstTicksMatch() throws Exception {
        for (JsonObject set : WireVectors.sets(WireVectors.load())) {
            WireSeed seed = seedOf(set);
            JsonObject first = (JsonObject) set.array("windows").orElseThrow().get(0);
            double[][] plain = WireVectors.rows(first.get("plain_rows").orElseThrow());
            double[][] draws = WireVectors.rows(set.get("noise_first_two_ticks").orElseThrow());
            double[][] derived = WireVectors.rows(set.get("decoys_first_two_ticks").orElseThrow());
            JitterStream recordedDraws = new JitterStream(seed);
            for (double[] tick : draws) {
                for (double draw : tick) {
                    assertTrue(WireVectors.same(draw, recordedDraws.draw()), "jitter draw of " + seed);
                }
            }
            JitterStream jitter = new JitterStream(seed);
            DerivedColumns columns = new DerivedColumns();
            for (int tick = 0; tick < derived.length; tick++) {
                double[] produced = columns.derive(plain, tick, jitter);
                for (int column = 0; column < DerivedColumns.COUNT; column++) {
                    assertTrue(WireVectors.same(derived[tick][column], produced[column]),
                            seed + " tick " + tick + " decoy " + column + ": " + derived[tick][column] + " vs "
                                    + produced[column]);
                }
            }
        }
    }

    @Test
    void consecutiveWindowsOfOneSessionMatchBitForBit() throws Exception {
        int compared = 0;
        for (JsonObject set : WireVectors.sets(WireVectors.load())) {
            WireSeed seed = seedOf(set);
            ScrambledWindows session = new ScrambledWindows(seed);
            for (JsonValue entry : set.array("windows").orElseThrow().elements()) {
                JsonObject window = (JsonObject) entry;
                double[][] plain = WireVectors.rows(window.get("plain_rows").orElseThrow());
                double[][] sent = WireVectors.rows(window.get("transmitted_rows").orElseThrow());
                double[][] produced = session.scramble(new PlainWindow(plain));
                for (int tick = 0; tick < sent.length; tick++) {
                    for (int slot = 0; slot < ScrambledWindows.SLOTS; slot++) {
                        assertTrue(WireVectors.same(sent[tick][slot], produced[tick][slot]),
                                seed + " tick " + tick + " slot " + slot + ": " + sent[tick][slot] + " vs "
                                        + produced[tick][slot]);
                        compared++;
                    }
                }
            }
        }
        assertEquals(3 * 2 * PlainWindow.TICKS * ScrambledWindows.SLOTS, compared);
    }

    @Test
    void plainValuesAreTheShortestFloatDecimals() throws Exception {
        for (JsonObject set : WireVectors.sets(WireVectors.load())) {
            for (JsonValue entry : set.array("windows").orElseThrow().elements()) {
                for (double[] row : WireVectors.rows(((JsonObject) entry).get("plain_rows").orElseThrow())) {
                    for (double value : row) {
                        if (!Double.isNaN(value)) {
                            assertTrue(WireVectors.same(value, ShortestFloat.asWritten((float) value)), "" + value);
                        }
                    }
                }
            }
        }
    }

    private static WireSeed seedOf(JsonObject set) {
        return WireSeed.parse(set.string("seed").orElseThrow()).orElseThrow();
    }

    private static String next(SplitMix64 generator) {
        return Long.toUnsignedString(generator.next());
    }

    private static List<String> texts(JsonArray array) {
        List<String> texts = new ArrayList<>();
        for (JsonValue value : array.elements()) {
            texts.add(((JsonString) value).value());
        }
        return texts;
    }
}
