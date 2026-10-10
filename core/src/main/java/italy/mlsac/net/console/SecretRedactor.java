// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SecretRedactor {

    public static final String MASK = "<API key>";
    private static final int MIN_SECRET_LENGTH = 4;

    private final List<String> secrets = new CopyOnWriteArrayList<>();

    public void register(String secret) {
        if (secret != null && secret.trim().length() >= MIN_SECRET_LENGTH && !secrets.contains(secret)) {
            secrets.add(secret);
        }
    }

    public void clear() {
        secrets.clear();
    }

    public String redact(String text) {
        if (text == null || secrets.isEmpty()) {
            return text;
        }
        String result = text;
        for (String secret : secrets) {
            if (result.contains(secret)) {
                result = result.replace(secret, MASK);
            }
        }
        return result;
    }
}
