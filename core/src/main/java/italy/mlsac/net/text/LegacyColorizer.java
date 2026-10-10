// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegacyColorizer {

    private static final char SECTION = '§';
    private static final Pattern HEX_CODE = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final String LEGACY_CODES = "0123456789abcdefklmnorxABCDEFKLMNORX";

    private final HexColorNormalizer normalizer;

    public LegacyColorizer(HexColorNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public String colorize(String text) {
        String normalised = normalizer.normalise(text);
        return translateAmpersands(expandHex(normalised));
    }

    public String strip(String text) {
        String withoutHex = HEX_CODE.matcher(normalizer.normalise(text)).replaceAll("");
        String translated = translateAmpersands(withoutHex);
        StringBuilder out = new StringBuilder(translated.length());
        for (int i = 0; i < translated.length(); i++) {
            char c = translated.charAt(i);
            if (c == SECTION && i + 1 < translated.length()) {
                i++;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String expandHex(String text) {
        Matcher matcher = HEX_CODE.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            StringBuilder code = new StringBuilder().append(SECTION).append('x');
            for (char digit : matcher.group(1).toCharArray()) {
                code.append(SECTION).append(digit);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(code.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String translateAmpersands(String text) {
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && LEGACY_CODES.indexOf(chars[i + 1]) >= 0) {
                chars[i] = SECTION;
            }
        }
        return new String(chars);
    }
}
