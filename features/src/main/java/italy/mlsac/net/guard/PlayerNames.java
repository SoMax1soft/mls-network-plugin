// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import java.util.Optional;
import java.util.regex.Pattern;

public final class PlayerNames {

    private static final Pattern JAVA_NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
    private static final Pattern SAFE_PREFIX = Pattern.compile("^[A-Za-z0-9_.*~!#$%^+=]{1,4}$");
    private static final Pattern UUID_TEXT = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final BedrockPrefix bedrock;

    public PlayerNames(BedrockPrefix bedrock) {
        this.bedrock = bedrock;
    }

    public boolean plain(String name) {
        if (name == null) {
            return false;
        }
        if (JAVA_NAME.matcher(name).matches()) {
            return true;
        }
        Optional<String> prefix = bedrock.prefix().filter(PlayerNames::safePrefix);
        return prefix.isPresent() && name.startsWith(prefix.get())
                && JAVA_NAME.matcher(name.substring(prefix.get().length())).matches();
    }

    public boolean uuid(String text) {
        return text != null && UUID_TEXT.matcher(text).matches();
    }

    static boolean safePrefix(String prefix) {
        return SAFE_PREFIX.matcher(prefix).matches() && !prefix.startsWith("-");
    }
}
