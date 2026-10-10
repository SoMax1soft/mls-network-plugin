// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.preset;

import italy.mlsac.net.backend.http.HttpCall;
import italy.mlsac.net.backend.http.HttpTransport;
import italy.mlsac.net.backend.http.TransportFailure;
import italy.mlsac.net.backend.request.BackendHeaders;
import italy.mlsac.net.config.core.PresetSelection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class PresetFetcher {

    public static final int MAX_BODY_BYTES = 256 * 1024;
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final String PATH = "/api/v1/plugin/config";

    private final HttpTransport transport;
    private final String primaryBase;
    private final PresetAnswerReader reader = new PresetAnswerReader();

    public PresetFetcher(HttpTransport transport, String primaryBase) {
        this.transport = transport;
        this.primaryBase = primaryBase;
    }

    public URI uri(PresetSelection selection) {
        String query = selection.queryName()
                .map(name -> "?preset=" + URLEncoder.encode(name, StandardCharsets.UTF_8))
                .orElse("");
        return URI.create(primaryBase + PATH + query);
    }

    public CompletableFuture<PresetAnswer> fetch(PresetSelection selection, String accessKey) {
        BackendHeaders headers = new BackendHeaders("", accessKey);
        HttpCall call = HttpCall.get(uri(selection), headers.presetFetch(), READ_TIMEOUT, MAX_BODY_BYTES);
        return transport.send(call).handle((reply, error) -> error != null
                ? new PresetAnswer.Failed("transport: " + TransportFailure.of(error).kind(), false)
                : reader.read(reply));
    }
}
