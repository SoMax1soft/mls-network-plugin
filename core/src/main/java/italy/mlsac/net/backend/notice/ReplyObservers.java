// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.notice;

import italy.mlsac.net.backend.session.ReplyInspector;
import italy.mlsac.net.json.JsonObject;

public final class ReplyObservers implements ReplyInspector {

    private final SharedNameAlarm duplicateNames;
    private final InterServerFeed interServer;

    public ReplyObservers(SharedNameAlarm duplicateNames, InterServerFeed interServer) {
        this.duplicateNames = duplicateNames;
        this.interServer = interServer;
    }

    @Override
    public void warnings(JsonObject reply) {
        duplicateNames.inspect(reply);
    }

    @Override
    public void interServerEvents(JsonObject reply) {
        interServer.accept(reply);
    }
}
