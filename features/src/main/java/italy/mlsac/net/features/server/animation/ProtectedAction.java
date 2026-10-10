// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

public record ProtectedAction<E extends Event>(Class<E> kind, EventPriority priority,
        Function<? super E, Stream<? extends Entity>> involved, Countermeasure<? super E> countermeasure) {

    public void enforce(Event event, Predicate<UUID> animating) {
        if (!kind.isInstance(event)) {
            return;
        }
        E typed = kind.cast(event);
        involved.apply(typed)
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .filter(candidate -> animating.test(candidate.getUniqueId()))
                .findFirst()
                .ifPresent(suspect -> countermeasure.answer(typed, suspect));
    }
}
