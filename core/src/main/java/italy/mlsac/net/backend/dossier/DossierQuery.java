// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.dossier;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public record DossierQuery(DossierKind kind, Map<String, String> parameters) {

    public DossierQuery {
        parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }

    public static DossierQuery of(DossierKind kind) {
        return new DossierQuery(kind, Map.of());
    }

    public DossierQuery with(String name, Object value) {
        Map<String, String> next = new LinkedHashMap<>(parameters);
        next.put(name, String.valueOf(value));
        return new DossierQuery(kind, next);
    }

    public String path() {
        StringJoiner query = new StringJoiner("&", "?", "").setEmptyValue("");
        parameters.forEach((name, value) -> query.add(URLEncoder.encode(name, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8)));
        return "plugin/dossier/" + kind.path() + query;
    }
}
