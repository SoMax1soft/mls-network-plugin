// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.notice;

import italy.mlsac.net.json.JsonObject;
import java.util.Optional;

record DuplicateNameNotice(boolean active, Optional<String> serverText) {

    private static final DuplicateNameNotice QUIET = new DuplicateNameNotice(false, Optional.empty());

    static DuplicateNameNotice readFrom(JsonObject reply) {
        return reply.object("warnings")
                .flatMap(warnings -> warnings.object("duplicateServerName"))
                .map(block -> new DuplicateNameNotice(block.bool("active").orElse(false),
                        block.string("message").filter(text -> !text.isBlank())))
                .orElse(QUIET);
    }

    String textOr(String fallback) {
        return serverText.orElse(fallback);
    }
}
