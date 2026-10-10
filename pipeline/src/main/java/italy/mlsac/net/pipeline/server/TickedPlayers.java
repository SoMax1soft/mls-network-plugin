// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.server;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.lifecycle.PlayerTick;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.pipeline.live.ServerHooks;
import italy.mlsac.net.player.PlayerLeft;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.entity.Player;

public final class TickedPlayers implements ServerHooks {

    private final EventBus bus;
    private final ServerRoster roster;
    private final List<PlayerTickWork> works;
    private final List<Subscription> subscriptions = new ArrayList<>();

    public TickedPlayers(EventBus bus, ServerRoster roster, List<PlayerTickWork> works) {
        this.bus = bus;
        this.roster = roster;
        this.works = List.copyOf(works);
    }

    @Override
    public void start() {
        Stream.of(
                bus.subscribe(ServerTick.class, tick -> roster.online().forEach(player -> perform(player, tick.tick()))),
                bus.subscribe(PlayerTick.class, this::single),
                bus.subscribe(PlayerLeft.class, left -> works.forEach(work -> work.forget(left.player()))))
                .forEach(subscriptions::add);
    }

    @Override
    public void stop() {
        subscriptions.forEach(Subscription::cancel);
        subscriptions.clear();
    }

    private void single(PlayerTick tick) {
        roster.player(tick.player()).ifPresent(player -> perform(player, tick.tick()));
    }

    private void perform(Player player, long tick) {
        works.forEach(work -> work.perform(player, tick));
    }
}
