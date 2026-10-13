// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

import italy.mlsac.net.backend.dossier.DossierQuery;
import italy.mlsac.net.backend.dossier.DossierReply;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface DossierSource {

    CompletableFuture<DossierReply> fetch(DossierQuery query);
}
