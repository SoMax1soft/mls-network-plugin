// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class TemplateValues {

    private static final TemplateValues NONE = new TemplateValues(List.of());

    private final List<Map.Entry<String, String>> replacements;

    private TemplateValues(List<Map.Entry<String, String>> replacements) {
        this.replacements = Collections.unmodifiableList(replacements);
    }

    public static TemplateValues none() {
        return NONE;
    }

    public static TemplateValues of(String placeholder, Object value) {
        return NONE.and(placeholder, value);
    }

    public TemplateValues and(String placeholder, Object value) {
        List<Map.Entry<String, String>> next = new ArrayList<>(replacements);
        next.add(Map.entry(placeholder, String.valueOf(value)));
        return new TemplateValues(next);
    }

    public TemplateValues aliases(Object value, String... placeholders) {
        TemplateValues result = this;
        for (String placeholder : placeholders) {
            result = result.and(placeholder, value);
        }
        return result;
    }

    public String apply(String template) {
        String result = template;
        for (Map.Entry<String, String> replacement : replacements) {
            result = result.replace(replacement.getKey(), replacement.getValue());
        }
        return result;
    }
}
