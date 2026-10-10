// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend;

import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class LoopbackOverride {

    public static final String PROPERTY = "mlsac.backend.override";

    private static final Set<String> LOCAL_ADDRESSES = Set.of("127.0.0.1", "localhost", "[::1]", "::1");
    private static final Set<String> SCHEMES = Set.of("http", "https");

    private final Optional<String> requested;

    public LoopbackOverride(String requestedRaw) {
        this.requested = Optional.ofNullable(requestedRaw).map(String::trim).filter(value -> !value.isEmpty());
    }

    public static LoopbackOverride fromSystemProperty() {
        return new LoopbackOverride(System.getProperty(PROPERTY));
    }

    public Optional<String> requested() {
        return requested;
    }

    public boolean accepted() {
        return requested.filter(LoopbackOverride::loopback).isPresent();
    }

    public BackendAddress address() {
        return accepted() ? BackendAddress.of(requested.get(), "") : BackendAddress.production();
    }

    public BackendAddress resolve(PluginLog log) {
        requested.ifPresent(raw -> log.warn(LogTopic.BACKEND, accepted()
                ? "TEST MODE: -D" + PROPERTY + " is set, every backend request goes to " + raw
                        + " instead of the production servers"
                : "-D" + PROPERTY + "=" + raw + " ignored: only http(s) loopback addresses are allowed"));
        return address();
    }

    private static boolean loopback(String raw) {
        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            return SCHEMES.contains(scheme) && LOCAL_ADDRESSES.contains(host) && uri.getRawUserInfo() == null;
        } catch (URISyntaxException invalid) {
            return false;
        }
    }
}
