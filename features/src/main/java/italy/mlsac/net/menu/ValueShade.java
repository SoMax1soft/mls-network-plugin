// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.text.DisplayNumbers;
import java.util.NavigableMap;
import java.util.TreeMap;

public record ValueShade(String color, String pane) {

    private static final ValueShade CALM = new ValueShade("§a", "GREEN_STAINED_GLASS_PANE");
    private static final NavigableMap<Double, ValueShade> FROM_LOWER_BOUND = new TreeMap<>();

    static {
        FROM_LOWER_BOUND.put(Double.NEGATIVE_INFINITY, CALM);
        FROM_LOWER_BOUND.put(0.4, new ValueShade("§e", "LIME_STAINED_GLASS_PANE"));
        FROM_LOWER_BOUND.put(0.6, new ValueShade("§6", "YELLOW_STAINED_GLASS_PANE"));
        FROM_LOWER_BOUND.put(0.8, new ValueShade("§c", "ORANGE_STAINED_GLASS_PANE"));
        FROM_LOWER_BOUND.put(0.9, new ValueShade("§4", "RED_STAINED_GLASS_PANE"));
    }

    public static ValueShade of(double value) {
        return Double.isNaN(value) ? CALM : FROM_LOWER_BOUND.floorEntry(value).getValue();
    }

    public static String twoDecimals(double value) {
        return tinted(value, DisplayNumbers.twoDecimals(value));
    }

    public static String roundedPercent(double value) {
        return tinted(value, DisplayNumbers.roundedPercent(value) + "%");
    }

    private static String tinted(double value, String shown) {
        return of(value).color() + shown + "§r";
    }
}
