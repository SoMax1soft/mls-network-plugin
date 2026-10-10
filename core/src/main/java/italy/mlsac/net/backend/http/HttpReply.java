// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import java.nio.charset.StandardCharsets;

public record HttpReply(int status, String contentType, byte[] body, long headerMillis) {

    public HttpReply {
        contentType = contentType == null ? "" : contentType;
        body = body == null ? new byte[0] : body.clone();
    }

    public static HttpReply of(int status, String body) {
        return new HttpReply(status, "application/json", body.getBytes(StandardCharsets.UTF_8), 0);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    public String text() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public boolean success() {
        return status >= 200 && status < 300;
    }
}
