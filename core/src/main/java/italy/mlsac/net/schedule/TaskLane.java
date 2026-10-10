// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.schedule;

import java.util.UUID;

public sealed interface TaskLane permits TaskLane.Main, TaskLane.Background, TaskLane.Follow, TaskLane.Chunk {

    TaskLane MAIN = new Main();
    TaskLane BACKGROUND = new Background();

    static TaskLane following(UUID player) {
        return new Follow(player, () -> { });
    }

    static TaskLane following(UUID player, Runnable whenGone) {
        return new Follow(player, whenGone);
    }

    static TaskLane chunkAt(UUID world, int blockX, int blockZ) {
        return new Chunk(world, blockX, blockZ);
    }

    <R> R route(LaneRoutes<R> routes);

    record Main() implements TaskLane {
        @Override
        public <R> R route(LaneRoutes<R> routes) {
            return routes.main();
        }
    }

    record Background() implements TaskLane {
        @Override
        public <R> R route(LaneRoutes<R> routes) {
            return routes.background();
        }
    }

    record Follow(UUID player, Runnable whenGone) implements TaskLane {
        @Override
        public <R> R route(LaneRoutes<R> routes) {
            return routes.player(player, whenGone);
        }
    }

    record Chunk(UUID world, int blockX, int blockZ) implements TaskLane {
        @Override
        public <R> R route(LaneRoutes<R> routes) {
            return routes.chunk(world, blockX, blockZ);
        }
    }
}
