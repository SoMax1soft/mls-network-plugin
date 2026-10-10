// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import italy.mlsac.net.backend.http.HttpHeader;
import java.util.ArrayList;
import java.util.List;

public final class BackendHeaders {

    public static final String API_KEY = "X-API-Key";
    public static final String PLUGIN_VERSION = "X-MLSAC-Plugin-Version";
    public static final String GENERATION_MARK = "X-MLSAC-Plugin-Generation";

    private final String buildVersion;
    private final String accessKey;

    public BackendHeaders(String buildVersion, String accessKey) {
        this.buildVersion = buildVersion;
        this.accessKey = accessKey;
    }

    public List<HttpHeader> withKey() {
        List<HttpHeader> headers = new ArrayList<>(3);
        headers.add(new HttpHeader(API_KEY, accessKey));
        headers.addAll(withoutKey());
        return List.copyOf(headers);
    }

    public List<HttpHeader> withoutKey() {
        return List.of(new HttpHeader(PLUGIN_VERSION, buildVersion),
                new HttpHeader(GENERATION_MARK, Integer.toString(RequestIdentity.GENERATION_MARK)));
    }

    public List<HttpHeader> presetFetch() {
        return List.of(new HttpHeader(API_KEY, accessKey),
                new HttpHeader(GENERATION_MARK, Integer.toString(RequestIdentity.GENERATION_MARK)));
    }
}
