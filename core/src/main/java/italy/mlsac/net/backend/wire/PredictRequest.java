// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.wire;

import italy.mlsac.net.backend.http.HttpHeader;
import java.util.List;

public record PredictRequest(int wireVersion, byte[] body, List<HttpHeader> extraHeaders) {

    public PredictRequest {
        body = body.clone();
        extraHeaders = List.copyOf(extraHeaders);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
