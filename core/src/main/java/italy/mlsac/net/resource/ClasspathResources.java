// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

public final class ClasspathResources implements ResourceSource {

    private final ClassLoader loader;

    public ClasspathResources(ClassLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    @Override
    public Optional<String> text(String path) {
        return bytes(path).map(data -> new String(data, StandardCharsets.UTF_8));
    }

    @Override
    public Optional<byte[]> bytes(String path) {
        try (InputStream in = loader.getResourceAsStream(path)) {
            return in == null ? Optional.empty() : Optional.of(in.readAllBytes());
        } catch (IOException failure) {
            throw new UncheckedIOException("cannot read bundled resource " + path, failure);
        }
    }
}
