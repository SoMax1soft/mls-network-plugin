// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.player;

public final class GameModeCode {
    private GameModeCode() {
    }

    public static int of(String playModeName) {
        if (playModeName == null) {
            return -1;
        }
        return switch (playModeName) {
            case "SURVIVAL" -> 0;
            case "CREATIVE" -> 1;
            case "ADVENTURE" -> 2;
            case "SPECTATOR" -> 3;
            default -> -1;
        };
    }
}
