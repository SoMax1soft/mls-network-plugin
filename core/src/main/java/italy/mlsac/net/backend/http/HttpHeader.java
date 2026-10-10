// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import java.util.Objects;

public record HttpHeader(String name, String value) {

    public HttpHeader {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
    }
}
