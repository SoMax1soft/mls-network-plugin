// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record BackendAddress(String primary, Optional<String> reserve) {

    public static final String PRODUCTION_PRIMARY = "https://api.mlsac.net";
    public static final String PRODUCTION_RESERVE = "https://ruapi.mlsac.net";
    private static final String API_ROOT = "/api/v1/";

    public BackendAddress {
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(reserve, "reserve");
    }

    public static BackendAddress production() {
        return of(PRODUCTION_PRIMARY, PRODUCTION_RESERVE);
    }

    public static BackendAddress of(String primaryRaw, String reserveRaw) {
        String primary = normalise(primaryRaw);
        String reserve = normalise(reserveRaw);
        boolean reserveDistinct = !reserve.isEmpty() && !reserve.equals(primary);
        return new BackendAddress(primary, reserveDistinct ? Optional.of(reserve) : Optional.empty());
    }

    public String base(Endpoint endpoint) {
        return endpoint == Endpoint.RESERVE ? reserve.orElse(primary) : primary;
    }

    public URI uri(Endpoint endpoint, String path) {
        return URI.create(base(endpoint) + API_ROOT + path);
    }

    public boolean reserveDistinct() {
        return reserve.isPresent();
    }

    public boolean plaintextRemote() {
        String lower = primary.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") && !lower.contains("localhost") && !lower.contains("127.0.0.1");
    }

    private static String normalise(String raw) {
        String value = raw == null ? "" : raw.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
