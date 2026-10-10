// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.notice;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class Placeholders {

    private final Map<String, String> values = new LinkedHashMap<>();

    public Placeholders text(String name, String value) {
        values.put("{" + name + "}", value);
        return this;
    }

    public Placeholders integer(String name, int value) {
        return text(name, Integer.toString(value));
    }

    public Placeholders oneDecimal(String name, double value) {
        return text(name, String.format(Locale.ROOT, "%.1f", value));
    }

    public Placeholders twoDecimals(String name, double value) {
        return text(name, String.format(Locale.ROOT, "%.2f", value));
    }

    public String applyTo(String template) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
