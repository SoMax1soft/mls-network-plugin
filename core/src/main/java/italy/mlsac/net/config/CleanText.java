// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.config;

public final class CleanText {

    private CleanText() {
    }

    public static String of(String raw, int maxLength) {
        StringBuilder out = new StringBuilder(Math.min(raw.length(), maxLength));
        for (int i = 0; i < raw.length() && out.length() < maxLength; i++) {
            char c = raw.charAt(i);
            if (c >= 0x20 && c != 0x7F) {
                out.append(c);
            }
        }
        return out.toString().trim();
    }
}
