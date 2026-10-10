// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import italy.mlsac.net.pipeline.engine.PipelineInput;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.input.SwingPacket;
import italy.mlsac.net.pipeline.input.TickEndPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

final class ArenaFight {
    static final long TICK_NANOS = 50_000_000L;
    private static final int FIRST_ENTITY = 4000;
    private static final double PAIR_SPACING = 6.0;
    private static final double ORBIT = 1.4;

    private final ArenaServer server;
    private final List<UUID> players = new ArrayList<>();

    ArenaFight(ArenaServer server, int fighters) {
        this.server = server;
        for (int index = 0; index < fighters; index++) {
            UUID player = new UUID(0xA7E7AL, index + 1L);
            players.add(player);
            server.connect(player, FIRST_ENTITY + index);
        }
    }

    List<UUID> players() {
        return players;
    }

    void tickServer(PipelineInput input, long tick) {
        for (int index = 0; index < players.size(); index++) {
            double[] spot = spot(index, tick);
            server.place(players.get(index), tick * TICK_NANOS, spot[0], spot[1], 0f, 0f);
        }
        server.at(tick * TICK_NANOS);
        input.gameTick(tick);
    }

    void tickClient(PipelineInput input, int index, long tick) {
        UUID player = players.get(index);
        int opponent = index ^ 1;
        Random jitter = new Random(index * 100_003L + tick);
        long base = tick * TICK_NANOS;
        double[] own = spot(index, tick);
        double[] other = spot(opponent, tick);
        double dx = other[0] - own[0];
        double dz = other[1] - own[1];
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)) + jitter.nextGaussian() * 3.0);
        float pitch = (float) (8.0 + jitter.nextGaussian() * 2.0);
        long moveAt = base + 3_000_000L + (index % 5) * 1_000_000L;
        server.at(moveAt);
        input.movement(new MovementPacket(player, moveAt, yaw, pitch, true, own[0], 64.0, own[1], true, true, false));
        if ((tick + index) % 7 == 0) {
            long swingAt = base + 20_000_000L;
            server.at(swingAt);
            input.swing(new SwingPacket(player, swingAt, true));
            input.attack(new AttackPacket(player, swingAt, FIRST_ENTITY + opponent, false));
        }
        if (jitter.nextInt(4) == 0) {
            long endAt = base + 45_000_000L;
            server.at(endAt);
            input.clientTickEnd(new TickEndPacket(player, endAt, "V_1_21_4"));
        }
    }

    private double[] spot(int index, long tick) {
        int pair = index / 2;
        double angle = tick * 0.05 + pair + (index % 2 == 0 ? 0.0 : Math.PI);
        double centreX = pair % 8 * PAIR_SPACING;
        double centreZ = pair / 8 * PAIR_SPACING;
        return new double[] {centreX + ORBIT * Math.cos(angle), centreZ + ORBIT * Math.sin(angle)};
    }
}
