// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.dossier;

public record KickEntry(String player, long at, String action, String model, double probability, int kicks,
                        int bans) {
}
