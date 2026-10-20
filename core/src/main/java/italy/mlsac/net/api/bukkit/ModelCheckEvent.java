// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.api.bukkit;

import italy.mlsac.net.api.ModelChoice;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class ModelCheckEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private static final Set<ModelChoice> REPORTED_MODELS = EnumSet.of(ModelChoice.PRO, ModelChoice.MINI);

    private final UUID playerId;
    private final String playerName;
    private final ModelChoice model;
    private final double probability;
    private final double buffer;
    private final boolean flagged;

    public ModelCheckEvent(UUID playerId, String playerName, ModelChoice model, double probability, double buffer,
                           boolean flagged) {
        super(true);
        if (!REPORTED_MODELS.contains(model)) {
            throw new IllegalArgumentException("Check results are reported for Pro and Mini only, not " + model);
        }
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.model = model;
        this.probability = probability;
        this.buffer = buffer;
        this.flagged = flagged;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public ModelChoice getModel() {
        return model;
    }

    public double getProbability() {
        return probability;
    }

    public double getBuffer() {
        return buffer;
    }

    public boolean isFlagged() {
        return flagged;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
