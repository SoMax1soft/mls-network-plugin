// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.suppression.server.bukkit.motion;

import italy.mlsac.net.suppression.server.bukkit.cadence.LagSubject;
import italy.mlsac.net.suppression.server.bukkit.cadence.LagSubjects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BukkitLagSubjects implements LagSubjects {

    private final BodyConditions conditions;
    private final TeleportRoute route;
    private final ViewTurn viewTurn;

    public BukkitLagSubjects(BodyConditions conditions, TeleportRoute route, ViewTurn viewTurn) {
        this.conditions = conditions;
        this.route = route;
        this.viewTurn = viewTurn;
    }

    @Override
    public Optional<LagSubject> subjectOf(UUID suspectId) {
        Player player = Bukkit.getPlayer(suspectId);
        if (player == null || !player.isOnline()) {
            return Optional.empty();
        }
        return Optional.of(new BukkitLagSubject(player, conditions, new BukkitLagActuator(player, route, viewTurn)));
    }
}
