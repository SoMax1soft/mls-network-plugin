// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.history;

public final class PlayerNameSanitizer {
    private static final int MAXIMUM_LENGTH = 32;
    private static final String FALLBACK = "unknown";

    private PlayerNameSanitizer() {
    }

    public static String sanitise(String name) {
        if (name == null || name.isEmpty()) {
            return FALLBACK;
        }
        StringBuilder safe = new StringBuilder(name.length());
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            boolean allowed = (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z')
                    || (character >= '0' && character <= '9') || character == '_' || character == '.'
                    || character == '-';
            safe.append(allowed ? character : '_');
        }
        String result = safe.toString();
        while (result.contains("..")) {
            result = result.replace("..", "_");
        }
        if (result.length() > MAXIMUM_LENGTH) {
            result = result.substring(0, MAXIMUM_LENGTH);
        }
        return result.isEmpty() ? FALLBACK : result;
    }
}
