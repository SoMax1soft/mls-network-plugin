// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.platform;

@FunctionalInterface
public interface ClassProbe {

    boolean isPresent(String className);

    static ClassProbe of(ClassLoader loader) {
        return className -> {
            try {
                Class.forName(className, false, loader);
                return true;
            } catch (Throwable absent) {
                return false;
            }
        };
    }
}
