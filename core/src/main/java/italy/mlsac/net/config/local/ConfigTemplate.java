// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config.local;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigTemplate {

    private static final Pattern KEY_LINE = Pattern.compile("^( *)([A-Za-z0-9_-]+):(.*)$");

    private final String text;

    public ConfigTemplate(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    public List<String> scalarPaths() {
        List<String> paths = new ArrayList<>();
        walk((path, indent, key, isScalar, index) -> {
            if (isScalar) {
                paths.add(path);
            }
        });
        return paths;
    }

    public String withValues(Map<String, Object> values) {
        String[] lines = text.split("\n", -1);
        walk((path, indent, key, isScalar, index) -> {
            if (isScalar && values.containsKey(path)) {
                lines[index] = " ".repeat(indent) + key + ": " + render(values.get(path));
            }
        });
        return String.join("\n", lines);
    }

    static String render(Object value) {
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        if (value instanceof List<?> list) {
            List<String> items = new ArrayList<>();
            list.forEach(item -> items.add(render(item)));
            return "[" + String.join(", ", items) + "]";
        }
        String raw = String.valueOf(value);
        StringBuilder quoted = new StringBuilder("\"");
        for (char c : raw.toCharArray()) {
            if (c == '"' || c == '\\') {
                quoted.append('\\');
            }
            quoted.append(c);
        }
        return quoted.append('"').toString();
    }

    private void walk(LineVisitor visitor) {
        String[] lines = text.split("\n", -1);
        Deque<int[]> indents = new ArrayDeque<>();
        Deque<String> keys = new ArrayDeque<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            Matcher matcher = KEY_LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            int indent = matcher.group(1).length();
            while (!indents.isEmpty() && indents.peek()[0] >= indent) {
                indents.pop();
                keys.pop();
            }
            String key = matcher.group(2);
            String rest = matcher.group(3).trim();
            String path = keys.isEmpty() ? key : String.join(".", reversed(keys)) + "." + key;
            boolean isScalar = !rest.isEmpty() && !rest.startsWith("#");
            visitor.visit(path, indent, key, isScalar, i);
            if (!isScalar) {
                indents.push(new int[] {indent});
                keys.push(key);
            }
        }
    }

    private static List<String> reversed(Deque<String> keys) {
        List<String> ordered = new ArrayList<>(keys);
        Collections.reverse(ordered);
        return ordered;
    }

    @FunctionalInterface
    private interface LineVisitor {
        void visit(String path, int indent, String key, boolean isScalar, int lineIndex);
    }
}
