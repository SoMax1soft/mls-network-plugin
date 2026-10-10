// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import italy.mlsac.net.backend.http.HttpHeader;
import java.util.ArrayList;
import java.util.List;

public record PackedBody(byte[] bytes, List<HttpHeader> headers, boolean packed) {

    public static final int SMALLEST_WORTH_PACKING = 1024;
    public static final HttpHeader GZIP = new HttpHeader("Content-Encoding", "gzip");

    public static PackedBody of(byte[] plain, List<HttpHeader> headers) {
        if (plain.length < SMALLEST_WORTH_PACKING || alreadyEncoded(headers)) {
            return new PackedBody(plain, headers, false);
        }
        byte[] gzipped = GzipBody.compress(plain);
        if (gzipped.length >= plain.length) {
            return new PackedBody(plain, headers, false);
        }
        List<HttpHeader> withEncoding = new ArrayList<>(headers.size() + 1);
        withEncoding.addAll(headers);
        withEncoding.add(GZIP);
        return new PackedBody(gzipped, withEncoding, true);
    }

    private static boolean alreadyEncoded(List<HttpHeader> headers) {
        for (HttpHeader header : headers) {
            if (header.name().equalsIgnoreCase(GZIP.name())) {
                return true;
            }
        }
        return false;
    }
}
