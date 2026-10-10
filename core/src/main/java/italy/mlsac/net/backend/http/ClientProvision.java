// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.http;

import java.net.http.HttpClient;

public interface ClientProvision {

    HttpClient acquire();

    void release();
}
