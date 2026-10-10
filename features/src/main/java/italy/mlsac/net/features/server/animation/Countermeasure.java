// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.features.server.animation;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

@FunctionalInterface
public interface Countermeasure<E extends Event> {

    void answer(E event, Player suspect);

    default Countermeasure<E> then(Countermeasure<? super E> next) {
        return (event, suspect) -> {
            answer(event, suspect);
            next.answer(event, suspect);
        };
    }

    static <E extends Event & Cancellable> Countermeasure<E> veto() {
        return (event, suspect) -> event.setCancelled(true);
    }
}
