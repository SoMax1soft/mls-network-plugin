// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.dossier;

import italy.mlsac.net.json.JsonObject;

public sealed interface DossierReply permits DossierReply.Found, DossierReply.Refused, DossierReply.Unavailable,
        DossierReply.Busy {

    int TOO_MANY = 429;
    int BAD_INPUT = 400;

    record Found(JsonObject data) implements DossierReply {
    }

    record Refused(int status, String code) implements DossierReply {

        public boolean throttled() {
            return status == TOO_MANY;
        }

        public boolean badInput() {
            return status == BAD_INPUT;
        }
    }

    record Unavailable() implements DossierReply {
    }

    record Busy() implements DossierReply {
    }
}
