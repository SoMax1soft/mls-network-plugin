// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.api.bukkit;

import italy.mlsac.net.api.ModelSelection;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerModelChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ModelSelection previous;
    private final ModelSelection current;

    public PlayerModelChangeEvent(ModelSelection previous, ModelSelection current) {
        super(true);
        this.previous = Objects.requireNonNull(previous, "previous");
        this.current = Objects.requireNonNull(current, "current");
    }

    public UUID getPlayerId() {
        return current.player();
    }

    public ModelSelection getPrevious() {
        return previous;
    }

    public ModelSelection getCurrent() {
        return current;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
