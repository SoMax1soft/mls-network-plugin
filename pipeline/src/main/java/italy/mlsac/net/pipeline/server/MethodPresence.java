// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

final class MethodPresence {

    private MethodPresence() {
    }

    static boolean declares(Class<?> type, String method, Class<?>... parameters) {
        try {
            type.getMethod(method, parameters);
            return true;
        } catch (NoSuchMethodException absent) {
            return false;
        }
    }
}
