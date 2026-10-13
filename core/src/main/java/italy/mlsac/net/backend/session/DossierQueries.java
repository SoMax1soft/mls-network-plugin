// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import italy.mlsac.net.backend.http.HttpCall;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.json.JsonObject;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class DossierQueries {

    static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    static final int MAX_REPLY_BYTES = 64 * 1024;

    private final BackendContext context;
    private final ConnectionState state;

    DossierQueries(BackendContext context, ConnectionState state) {
        this.context = context;
        this.state = state;
    }

    CompletableFuture<DossierReply> ask(DossierQuery query) {
        if (!state.connected()) {
            return CompletableFuture.completedFuture(new DossierReply.Unavailable());
        }
        HttpCall call = HttpCall.get(context.address().uri(state.current(), query.path()), context.headers().withKey(),
                READ_TIMEOUT, MAX_REPLY_BYTES);
        return context.transport().send(call)
                .handle((reply, error) -> error == null ? read(reply) : new DossierReply.Unavailable());
    }

    static DossierReply read(HttpReply reply) {
        Optional<JsonObject> root = LenientJson.object(reply.text());
        if (reply.success()) {
            return root.filter(body -> body.bool("success").orElse(false))
                    .flatMap(body -> body.object("data"))
                    .<DossierReply>map(DossierReply.Found::new)
                    .orElseGet(DossierReply.Unavailable::new);
        }
        String code = root.flatMap(body -> body.object("error")).flatMap(error -> error.string("code")).orElse("");
        return new DossierReply.Refused(reply.status(), code);
    }
}
