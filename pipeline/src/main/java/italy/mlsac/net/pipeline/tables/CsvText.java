// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.tables;

import java.util.List;

public final class CsvText {
    private CsvText() {
    }

    public static String render(String header, List<List<String>> rows, String lineSeparator) {
        StringBuilder text = new StringBuilder(header.length() + rows.size() * 256);
        text.append(header).append(lineSeparator);
        for (List<String> row : rows) {
            text.append(String.join(",", row)).append(lineSeparator);
        }
        return text.toString();
    }
}
