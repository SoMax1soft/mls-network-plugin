// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import italy.mlsac.net.pipeline.json.JsonReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class FileDiff {
    private static final List<String> NEUTRAL_SESSION_KEYS =
            List.of("sampling", "clock", "normalization", "target_angular_velocity_unit", "aim_lead_definition",
                    "extra_feature_definitions", "target_point", "v2_sampling", "v2_attack_rotation", "v2_no_packet",
                    "v2_target", "v2_rotation", "v2_weapon", "v2_reach", "v2_walls", "v2_units");

    private FileDiff() {
    }

    static boolean equivalent(String path, byte[] expected, byte[] actual) {
        if (Arrays.equals(expected, actual)) {
            return true;
        }
        return path.endsWith("session.json") && sessionFieldsEqual(expected, actual)
                && masked(expected).equals(masked(actual));
    }

    static boolean sessionFieldsEqual(byte[] expected, byte[] actual) {
        Map<String, Object> left = JsonReader.parseObject(new String(expected, StandardCharsets.UTF_8));
        Map<String, Object> right = JsonReader.parseObject(new String(actual, StandardCharsets.UTF_8));
        if (!List.copyOf(left.keySet()).equals(List.copyOf(right.keySet()))) {
            return false;
        }
        for (String key : left.keySet()) {
            if (!NEUTRAL_SESSION_KEYS.contains(key) && !Objects.equals(String.valueOf(left.get(key)),
                    String.valueOf(right.get(key)))) {
                return false;
            }
        }
        return true;
    }

    static String describe(String path, byte[] expected, byte[] actual) {
        if (Arrays.equals(expected, actual)) {
            return "identical";
        }
        if (path.endsWith("session.json")) {
            if (!sessionFieldsEqual(expected, actual)) {
                return "session fields differ: " + sessionDifferences(expected, actual);
            }
            return masked(expected).equals(masked(actual))
                    ? "field-equal and byte-identical outside the neutral free-text values (OWNER_DECISIONS 11)"
                    : "field-equal, layout differs";
        }
        if (path.endsWith(".csv")) {
            return csvDifferences(new String(expected, StandardCharsets.UTF_8),
                    new String(actual, StandardCharsets.UTF_8));
        }
        return "bytes differ";
    }

    private static String masked(byte[] session) {
        String text = new String(session, StandardCharsets.UTF_8);
        for (String key : NEUTRAL_SESSION_KEYS) {
            text = text.replaceAll("\"" + key + "\": \"(\\\\.|[^\"\\\\])*\"", "\"" + key + "\": \"\"");
        }
        return text;
    }

    private static String sessionDifferences(byte[] expected, byte[] actual) {
        Map<String, Object> left = JsonReader.parseObject(new String(expected, StandardCharsets.UTF_8));
        Map<String, Object> right = JsonReader.parseObject(new String(actual, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (String key : left.keySet()) {
            if (!NEUTRAL_SESSION_KEYS.contains(key)
                    && !Objects.equals(String.valueOf(left.get(key)), String.valueOf(right.get(key)))) {
                out.append(key).append("=").append(left.get(key)).append(" vs ").append(right.get(key)).append("; ");
            }
        }
        return out.toString();
    }

    private static String csvDifferences(String expected, String actual) {
        String[] left = expected.split("\n", -1);
        String[] right = actual.split("\n", -1);
        String[] header = left[0].split(",", -1);
        Map<String, Integer> columns = new LinkedHashMap<>();
        int differingLines = 0;
        String firstLine = "";
        int common = Math.min(left.length, right.length);
        for (int index = 1; index < common; index++) {
            if (left[index].equals(right[index])) {
                continue;
            }
            differingLines++;
            String[] a = left[index].split(",", -1);
            String[] b = right[index].split(",", -1);
            StringBuilder detail = new StringBuilder();
            for (int column = 0; column < Math.max(a.length, b.length); column++) {
                String x = column < a.length ? a[column] : "<none>";
                String y = column < b.length ? b[column] : "<none>";
                if (!x.equals(y)) {
                    String name = column < header.length ? header[column] : "#" + column;
                    columns.merge(name, 1, Integer::sum);
                    if (detail.length() < 400) {
                        detail.append(name).append(": ").append(x).append(" vs ").append(y).append("; ");
                    }
                }
            }
            if (firstLine.isEmpty()) {
                firstLine = "first at line " + (index + 1) + ": " + detail;
            }
        }
        return "lines expected " + (left.length - 1) + " produced " + (right.length - 1) + ", differing lines "
                + differingLines + ", columns " + columns + " " + firstLine;
    }
}
