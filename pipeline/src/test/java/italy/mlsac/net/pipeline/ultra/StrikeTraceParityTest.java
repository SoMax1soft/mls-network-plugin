// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.ultra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StrikeTraceParityTest {
    private static final double TOLERANCE = 1e-4;
    private static final String[] SIGNALS = {"sn_2000", "attack_err_ratio", "on_target_200", "on_target_600"};

    @ParameterizedTest
    @ValueSource(strings = {"legit_travinka", "cheat_dimasik", "cheat_helix"})
    void everyStretchRowMatchesTheTrainingCode(String recording) throws IOException {
        StrikeTrace trace = new StrikeTrace();
        double[] worst = new double[SIGNALS.length];
        int[] finite = new int[SIGNALS.length];
        int rows = 0;
        String stretch = null;
        try (BufferedReader reader = open(recording)) {
            Map<String, Integer> at = header(reader.readLine());
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cells = line.split(",", -1);
                if (!cells[at.get("stretch")].equals(stretch)) {
                    trace.forgetShortTerm();
                    stretch = cells[at.get("stretch")];
                }
                StrikeReading reading = new StrikeReading(number(cells, at, "delta_yaw"),
                        number(cells, at, "delta_pitch"), number(cells, at, "rot_valid") == 1.0,
                        number(cells, at, "target_valid") == 1.0, (int) number(cells, at, "attacks"),
                        number(cells, at, "aim_error_yaw"), number(cells, at, "aim_error_pitch"),
                        number(cells, at, "primary_hit") == 1.0);
                double[] got = new double[SIGNALS.length];
                trace.observe(reading).copyInto(got, 0);
                for (int signal = 0; signal < SIGNALS.length; signal++) {
                    double want = number(cells, at, SIGNALS[signal]);
                    String where = recording + " row " + rows + " " + SIGNALS[signal];
                    assertEquals(Double.isFinite(want), Double.isFinite(got[signal]), where + ": validity, python "
                            + want + " java " + got[signal]);
                    if (Double.isFinite(want)) {
                        double gap = Math.abs(want - got[signal]);
                        assertTrue(gap <= TOLERANCE, where + ": python " + want + " java " + got[signal]);
                        worst[signal] = Math.max(worst[signal], gap);
                        finite[signal]++;
                    }
                }
                rows++;
            }
        }
        assertTrue(rows > 1000, recording + " holds only " + rows + " rows");
        StringBuilder report = new StringBuilder(recording).append(": ").append(rows).append(" rows");
        for (int signal = 0; signal < SIGNALS.length; signal++) {
            assertTrue(finite[signal] > 0, recording + " never computes " + SIGNALS[signal]);
            report.append(String.format(Locale.ROOT, ", %s %d valid max |diff| %.3g", SIGNALS[signal],
                    finite[signal], worst[signal]));
        }
        Path out = Path.of("build", "reports", "strike-parity.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.append(System.lineSeparator()), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static BufferedReader open(String recording) throws IOException {
        InputStream raw = StrikeTraceParityTest.class.getResourceAsStream("/strike-parity/" + recording + ".csv.gz");
        assertNotNull(raw, "missing parity rows for " + recording);
        return new BufferedReader(new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.US_ASCII));
    }

    private static Map<String, Integer> header(String line) {
        Map<String, Integer> at = new HashMap<>();
        String[] names = line.split(",", -1);
        for (int index = 0; index < names.length; index++) {
            at.put(names[index], index);
        }
        return at;
    }

    private static double number(String[] cells, Map<String, Integer> at, String column) {
        String text = cells[at.get(column)];
        return "nan".equals(text) ? Double.NaN : Double.parseDouble(text);
    }
}
