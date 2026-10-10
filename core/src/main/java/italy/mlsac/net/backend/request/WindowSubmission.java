// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.request;

import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

public final class WindowSubmission {

    private final UUID player;
    private final String playerNick;
    private final byte[] window;

    public WindowSubmission(UUID player, String playerNick, byte[] window) {
        this(window.clone(), player, playerNick);
    }

    private WindowSubmission(byte[] owned, UUID player, String playerNick) {
        this.player = Objects.requireNonNull(player, "player");
        this.playerNick = Objects.requireNonNull(playerNick, "playerName");
        this.window = owned;
    }

    public static WindowSubmission handedOver(UUID player, String playerNick, byte[] freshWindow) {
        return new WindowSubmission(freshWindow, player, playerNick);
    }

    public UUID player() {
        return player;
    }

    public String playerNick() {
        return playerNick;
    }

    public byte[] window() {
        return window.clone();
    }

    public int windowLength() {
        return window.length;
    }

    public String windowBase64() {
        return Base64.getEncoder().encodeToString(window);
    }
}
