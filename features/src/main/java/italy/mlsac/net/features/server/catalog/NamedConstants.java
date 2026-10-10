// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.catalog;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class NamedConstants<T> {

    private final String typeName;
    private final Class<T> expected;
    private final Map<String, Optional<T>> resolved = new ConcurrentHashMap<>();

    public NamedConstants(String typeName, Class<T> expected) {
        this.typeName = typeName;
        this.expected = expected;
    }

    public Optional<T> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return resolved.computeIfAbsent(name.trim().toUpperCase(Locale.ROOT), this::lookUp);
    }

    private Optional<T> lookUp(String upperName) {
        try {
            Method valueOf = Class.forName(typeName).getMethod("valueOf", String.class);
            Object constant = valueOf.invoke(null, upperName);
            return expected.isInstance(constant) ? Optional.of(expected.cast(constant)) : Optional.empty();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException notOnThisServer) {
            return Optional.empty();
        }
    }
}
