// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class PlaceBook implements PlayerTickWork {

    private final ThreadOwnership ownership;
    private final Map<UUID, PlayerPlace> remembered = new ConcurrentHashMap<>();

    public PlaceBook(ThreadOwnership ownership) {
        this.ownership = ownership;
    }

    public Optional<PlayerPlace> of(Player player) {
        if (ownership.ownsEntity(player)) {
            return Optional.of(PlayerPlace.of(player));
        }
        return Optional.ofNullable(remembered.get(player.getUniqueId()));
    }

    @Override
    public void perform(Player player, long tick) {
        remembered.put(player.getUniqueId(), PlayerPlace.of(player));
    }

    @Override
    public void forget(UUID player) {
        remembered.remove(player);
    }
}
