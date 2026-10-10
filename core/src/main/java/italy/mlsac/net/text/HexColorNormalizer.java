// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.text;

import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HexColorNormalizer {

    private static final String HEX = "[0-9A-Fa-f]";
    private static final String HEX_MARK = "&#";
    private static final int SPIGOT_DIGITS = 6;
    private static final Pattern SPIGOT_SEQUENCE = Pattern.compile(
            "[&§][xX]" + ("[&§](" + HEX + ")").repeat(SPIGOT_DIGITS));
    private static final Pattern TAG_FORMS = Pattern.compile(
            "<#(" + HEX + "{6})>|<color:#(" + HEX + "{6})>|\\{#(" + HEX + "{6})}");
    private static final Pattern CLOSING_TAGS = Pattern.compile("</#" + HEX + "{6}>|</color>");
    private static final Pattern SHORT_FORM = Pattern.compile(
            "[&§]#(" + HEX + ")(" + HEX + ")(" + HEX + ")(?!" + HEX + ")");
    private static final Pattern SECTION_FORM = Pattern.compile("§#(" + HEX + "{6})");
    private static final Pattern BARE_FORM = Pattern.compile(
            "(^|[^&§A-Za-z0-9])#(" + HEX + "{6})(?!" + HEX + ")");

    public String normalise(String text) {
        if (text.indexOf('#') < 0 && text.indexOf('x') < 0 && text.indexOf('X') < 0) {
            return text;
        }
        String result = rewrite(text, SPIGOT_SEQUENCE, HexColorNormalizer::joinedDigits);
        result = rewrite(result, TAG_FORMS, match -> HEX_MARK + firstPresentGroup(match));
        result = rewrite(result, CLOSING_TAGS, match -> "");
        result = rewrite(result, SHORT_FORM, HexColorNormalizer::doubledDigits);
        result = rewrite(result, SECTION_FORM, match -> HEX_MARK + match.group(1));
        return rewrite(result, BARE_FORM, match -> match.group(1) + HEX_MARK + match.group(2));
    }

    private static String rewrite(String text, Pattern pattern, Function<MatchResult, String> replacement) {
        return pattern.matcher(text).replaceAll(match -> Matcher.quoteReplacement(replacement.apply(match)));
    }

    private static String joinedDigits(MatchResult match) {
        StringBuilder hex = new StringBuilder(HEX_MARK);
        for (int group = 1; group <= SPIGOT_DIGITS; group++) {
            hex.append(match.group(group));
        }
        return hex.toString();
    }

    private static String firstPresentGroup(MatchResult match) {
        for (int group = 1; group <= match.groupCount(); group++) {
            if (match.group(group) != null) {
                return match.group(group);
            }
        }
        return "";
    }

    private static String doubledDigits(MatchResult match) {
        StringBuilder hex = new StringBuilder(HEX_MARK);
        for (int group = 1; group <= match.groupCount(); group++) {
            hex.append(match.group(group)).append(match.group(group));
        }
        return hex.toString();
    }
}
