// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.dossier;

public enum DossierKind {
    PROFILE("profile"),
    KICKS("kicks"),
    HISTORY("history");

    private final String path;

    DossierKind(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
