// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.menu;

import italy.mlsac.net.backend.dossier.DossierReply;

public enum DossierTrouble {
    UNAVAILABLE("unavailable", "&cBackend unavailable, try again later"),
    THROTTLED("throttled", "&cToo many requests, try again in a minute"),
    INVALID("invalid", "&cInvalid player name"),
    BUSY("busy", "&7Still loading, wait a moment");

    private final String key;
    private final String fallback;

    DossierTrouble(String key, String fallback) {
        this.key = key;
        this.fallback = fallback;
    }

    public static DossierTrouble of(DossierReply reply) {
        if (reply instanceof DossierReply.Busy) {
            return BUSY;
        }
        if (reply instanceof DossierReply.Refused refused) {
            if (refused.throttled()) {
                return THROTTLED;
            }
            if (refused.badInput()) {
                return INVALID;
            }
        }
        return UNAVAILABLE;
    }

    public String key() {
        return key;
    }

    public String fallback() {
        return fallback;
    }
}
