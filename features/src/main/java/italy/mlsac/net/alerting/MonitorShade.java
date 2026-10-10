// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.alerting;

import italy.mlsac.net.text.DisplayNumbers;
import java.util.List;

public final class MonitorShade {

    private static final List<Band> BANDS = List.of(
            new Band(90, "&#FF0000"),
            new Band(80, "&#FF4500"),
            new Band(70, "&#FFA500"),
            new Band(60, "&#FFD700"),
            new Band(50, "&#FFFF00"),
            new Band(40, "&#90EE90"));
    private static final String LOWEST = "&#00FF00";

    private MonitorShade() {
    }

    public static String colored(double probability) {
        long percent = Long.parseLong(DisplayNumbers.truncatedPercent(probability));
        String color = BANDS.stream().filter(band -> percent >= band.from()).map(Band::color).findFirst().orElse(LOWEST);
        return color + percent + "%";
    }

    private record Band(long from, String color) {
    }
}
