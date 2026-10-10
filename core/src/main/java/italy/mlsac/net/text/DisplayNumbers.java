// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import java.util.Locale;

public final class DisplayNumbers {
    private static final double HUNDRED = 100.0;
    private static final double WHOLE_TOLERANCE = 0.0001;
    private static final String POINT_ZERO = ".0";

    private DisplayNumbers() {
    }

    public static String percent(double probability) {
        double scaled = probability * HUNDRED;
        double nearestWhole = Math.rint(scaled);
        return Math.abs(scaled - nearestWhole) < WHOLE_TOLERANCE
                ? Long.toString((long) nearestWhole)
                : withoutPointZero(places(scaled, 1));
    }

    public static String oneDecimal(double value) {
        return places(value, 1);
    }

    public static String bufferInServerLocale(double buffer) {
        return String.format(Locale.getDefault(Locale.Category.FORMAT), "%.1f", buffer);
    }

    public static String twoDecimals(double value) {
        return places(value, 2);
    }

    public static String truncatedPercent(double probability) {
        return String.valueOf((long) (probability * HUNDRED));
    }

    public static String roundedPercent(double probability) {
        return String.valueOf(Math.round(probability * HUNDRED));
    }

    private static String places(double value, int digits) {
        return String.format(Locale.ROOT, "%." + digits + "f", value);
    }

    private static String withoutPointZero(String text) {
        return text.endsWith(POINT_ZERO) ? text.substring(0, text.length() - POINT_ZERO.length()) : text;
    }
}
