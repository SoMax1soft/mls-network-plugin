// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.bus.EventBus;
import italy.mlsac.net.bus.Subscription;
import italy.mlsac.net.lifecycle.PlayerTick;
import italy.mlsac.net.lifecycle.ServerTick;
import italy.mlsac.net.penalty.AnimationStateChanged;
import italy.mlsac.net.pipeline.engine.PipelineInput;
import italy.mlsac.net.player.PlayerChangedWorld;
import italy.mlsac.net.player.PlayerDied;
import italy.mlsac.net.player.PlayerJoined;
import italy.mlsac.net.player.PlayerLeft;
import italy.mlsac.net.player.PlayerRespawned;
import italy.mlsac.net.player.PlayerTeleported;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class LifeFeed {

    private final EventBus bus;
    private final PipelineInput pipeline;
    private final AnimatingPlayers animations;
    private final List<Subscription> subscriptions = new ArrayList<>();

    public LifeFeed(EventBus bus, PipelineInput pipeline, AnimatingPlayers animations) {
        this.bus = bus;
        this.pipeline = pipeline;
        this.animations = animations;
    }

    public void start() {
        Stream.of(
                on(ServerTick.class, tick -> pipeline.gameTick(tick.tick())),
                on(PlayerTick.class, tick -> pipeline.playerTick(tick.player(), tick.tick())),
                on(PlayerJoined.class, joined -> pipeline.joined(joined.player())),
                on(PlayerLeft.class, left -> pipeline.quit(left.player())),
                on(PlayerDied.class, died -> pipeline.died(died.player())),
                on(PlayerRespawned.class, respawned -> pipeline.respawned(respawned.player())),
                on(PlayerChangedWorld.class, moved -> pipeline.worldSwitched(moved.player())),
                on(PlayerTeleported.class, teleported -> pipeline.teleported(teleported.player())),
                on(AnimationStateChanged.class, animations::changed))
                .forEach(subscriptions::add);
    }

    public void stop() {
        subscriptions.forEach(Subscription::cancel);
        subscriptions.clear();
        animations.clear();
    }

    private <E> Subscription on(Class<E> type, Consumer<? super E> reaction) {
        return bus.subscribe(type, reaction);
    }
}
