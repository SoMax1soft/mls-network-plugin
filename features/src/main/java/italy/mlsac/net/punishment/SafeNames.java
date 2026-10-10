// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.punishment;

import java.util.regex.Pattern;

public final class SafeNames {

    private static final Pattern SAFE = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");

    private SafeNames() {
    }

    public static boolean safe(String playerNick) {
        return playerNick != null && SAFE.matcher(playerNick).matches();
    }
}
