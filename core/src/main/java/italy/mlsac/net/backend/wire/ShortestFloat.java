// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class ShortestFloat {

    private static final int MAXIMUM_DIGITS = 9;
    private static final float EXACT_WHOLE_LIMIT = 0x1.0p24f;
    private static final MathContext[] PRECISIONS = precisions();
    private static final int POWER_OFFSET = 40;
    private static final double[] POWERS_OF_TEN = powersOfTen();
    private static final double TIE_MARGIN = 1 - 1e-6;
    static final boolean SHORTEST_TO_STRING = Runtime.version().feature() >= 19;

    private ShortestFloat() {
    }

    public static double asWritten(float value) {
        if (!Float.isFinite(value)) {
            return Double.NaN;
        }
        if (value == 0.0f) {
            return 0.0;
        }
        if (Math.abs(value) < EXACT_WHOLE_LIMIT && value == (float) Math.rint(value)) {
            return value;
        }
        if (SHORTEST_TO_STRING) {
            double fast = fromToString(value);
            if (!Double.isNaN(fast)) {
                return fast;
            }
        }
        return byRounding(value);
    }

    static double byRounding(float value) {
        BigDecimal exact = new BigDecimal((double) value);
        for (MathContext precision : PRECISIONS) {
            double candidate = Double.parseDouble(exact.round(precision).toString());
            if ((float) candidate == value) {
                return candidate;
            }
        }
        return value;
    }

    static double fromToString(float value) {
        String text = Float.toString(value);
        int end = text.length();
        int exponent = 0;
        int marker = text.indexOf('E');
        if (marker >= 0) {
            exponent = Integer.parseInt(text, marker + 1, text.length(), 10);
            end = marker;
        }
        int position = 0;
        int point = -1;
        int first = -1;
        int lastNonZero = -1;
        for (int i = 0; i < end; i++) {
            char c = text.charAt(i);
            if (c == '.') {
                point = position;
                continue;
            }
            if (c < '0' || c > '9') {
                continue;
            }
            if (first < 0 && c == '0') {
                position++;
                continue;
            }
            if (first < 0) {
                first = position;
            }
            if (c != '0') {
                lastNonZero = position;
            }
            position++;
        }
        if (point < 0) {
            point = position;
        }
        int digits = lastNonZero - first + 1;
        if (digits <= 2) {
            return Double.NaN;
        }
        int leading = point - first - 1 + exponent;
        int unit = leading - digits + 1;
        if (unit < -POWER_OFFSET || unit > POWERS_OF_TEN.length - 1 - POWER_OFFSET) {
            return Double.NaN;
        }
        double candidate = Double.parseDouble(text);
        double half = 0.5 * POWERS_OF_TEN[unit + POWER_OFFSET];
        if (Math.abs(candidate - (double) value) < half * TIE_MARGIN) {
            return candidate;
        }
        return Double.NaN;
    }

    private static MathContext[] precisions() {
        MathContext[] contexts = new MathContext[MAXIMUM_DIGITS];
        for (int digits = 1; digits <= MAXIMUM_DIGITS; digits++) {
            contexts[digits - 1] = new MathContext(digits, RoundingMode.HALF_UP);
        }
        return contexts;
    }

    private static double[] powersOfTen() {
        double[] powers = new double[80];
        for (int i = 0; i < powers.length; i++) {
            powers[i] = Double.parseDouble("1e" + (i - POWER_OFFSET));
        }
        return powers;
    }
}
