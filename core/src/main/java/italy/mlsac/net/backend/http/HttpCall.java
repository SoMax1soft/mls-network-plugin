// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record HttpCall(HttpMethod method, URI uri, List<HttpHeader> headers, Optional<String> contentType,
                       byte[] body, Duration readTimeout, int maxResponseBytes) {

    public static final String JSON_UTF8 = "application/json; charset=utf-8";

    public HttpCall {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(uri, "uri");
        headers = List.copyOf(headers);
        Objects.requireNonNull(contentType, "contentType");
        body = body == null ? new byte[0] : body.clone();
        Objects.requireNonNull(readTimeout, "readTimeout");
    }

    public static HttpCall postJson(URI uri, List<HttpHeader> headers, byte[] body, Duration readTimeout) {
        return new HttpCall(HttpMethod.POST, uri, headers, Optional.of(JSON_UTF8), body, readTimeout, Integer.MAX_VALUE);
    }

    public static HttpCall get(URI uri, List<HttpHeader> headers, Duration readTimeout, int maxResponseBytes) {
        return new HttpCall(HttpMethod.GET, uri, headers, Optional.empty(), new byte[0], readTimeout, maxResponseBytes);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
