// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.json.JsonObject;

public interface ReplyInspector {

    ReplyInspector NONE = new ReplyInspector() {
        @Override
        public void warnings(JsonObject reply) {
        }

        @Override
        public void interServerEvents(JsonObject reply) {
        }
    };

    void warnings(JsonObject reply);

    void interServerEvents(JsonObject reply);
}
