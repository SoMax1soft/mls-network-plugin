// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.live;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.pipeline.engine.DataPipeline;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.input.SwingPacket;
import italy.mlsac.net.pipeline.input.TickEndPacket;
import italy.mlsac.net.pipeline.player.PlayerPosture;
import italy.mlsac.net.pipeline.player.PlayerSample;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class StanceOnlySampleTest {
    private static final int FIGHTERS = 8;
    private static final int FIRST_ENTITY = 5000;
    private static final long TICKS = 600;
    private static final long TICK_NANOS = 50_000_000L;

    @Test
    void ultraWindowsIgnoreTheFieldsALightSampleLeavesOut() throws NoSuchAlgorithmException {
        String complete = windows(StanceOnlySampleTest::complete);
        assertEquals(complete, windows(StanceOnlySampleTest::stanceOnly));
        assertTrue(Integer.parseInt(complete.substring(0, complete.indexOf(':'))) > 20, complete);
    }

    @Test
    void ultraWindowsStillFollowTheFieldsALightSampleKeeps() throws NoSuchAlgorithmException {
        assertNotEquals(windows(StanceOnlySampleTest::complete),
                windows(sample -> withReach(complete(sample), 4.5)));
    }

    private static PlayerSample complete(PlayerSample base) {
        long tick = base.capturedAt() / TICK_NANOS;
        PlayerPosture posture = new PlayerPosture(tick % 4 == 0, tick % 9 == 0, tick % 5 == 0, tick % 3 == 0,
                tick % 7 == 0, false);
        String hand = tick % 11 < 6 ? "netherite_sword" : "diamond_axe";
        return new PlayerSample(base.player(), base.world(), base.entityNumber(), base.capturedAt(), base.x(),
                base.y(), base.z(), base.yaw(), base.pitch(), base.eyeLift(), base.height(), base.width(),
                base.fallBlocks(), base.swingCharge(), base.valid(), base.dead(), posture, hand, base.chargeTicks(),
                tick % 13 == 0 ? "ADVENTURE" : "SURVIVAL", 3.0);
    }

    private static PlayerSample stanceOnly(PlayerSample base) {
        PlayerSample full = complete(base);
        PlayerPosture posture = full.posture();
        return new PlayerSample(full.player(), full.world(), full.entityNumber(), full.capturedAt(), full.x(),
                full.y(), full.z(), full.yaw(), full.pitch(), full.eyeLift(), full.height(), full.width(),
                full.fallBlocks(), full.swingCharge(), full.valid(), full.dead(),
                new PlayerPosture(posture.sprinting(), posture.sneaking(), false, false, false, posture.riding()),
                "unknown", full.chargeTicks(), null, full.interactionRange());
    }

    private static PlayerSample withReach(PlayerSample full, double reach) {
        return new PlayerSample(full.player(), full.world(), full.entityNumber(), full.capturedAt(), full.x(),
                full.y(), full.z(), full.yaw(), full.pitch(), full.eyeLift(), full.height(), full.width(),
                full.fallBlocks(), full.swingCharge(), full.valid(), full.dead(), full.posture(), full.handMaterial(),
                full.chargeTicks(), full.playModeName(), reach);
    }

    private static String windows(UnaryOperator<PlayerSample> style) throws NoSuchAlgorithmException {
        ArenaServer server = new ArenaServer();
        server.restyle(style);
        PlayerOrderedInput input = new PlayerOrderedInput(new DataPipeline(server.ports()));
        List<UUID> players = new ArrayList<>();
        for (int index = 0; index < FIGHTERS; index++) {
            UUID player = new UUID(0x5A3B1EL, index + 1L);
            players.add(player);
            server.connect(player, FIRST_ENTITY + index);
        }
        input.enabled();
        for (long tick = 0; tick < TICKS; tick++) {
            for (int index = 0; index < FIGHTERS; index++) {
                double[] spot = spot(index, tick);
                server.place(players.get(index), tick * TICK_NANOS, spot[0], spot[1], 0f, 0f);
            }
            server.at(tick * TICK_NANOS);
            input.gameTick(tick);
            for (int index = 0; index < FIGHTERS; index++) {
                clientTick(server, input, players, index, tick);
            }
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int windows = 0;
        for (UUID player : players) {
            for (byte[] window : server.windowsOf(player)) {
                digest.update(window);
                windows++;
            }
        }
        return windows + ":" + HexFormat.of().formatHex(digest.digest());
    }

    private static void clientTick(ArenaServer server, PlayerOrderedInput input, List<UUID> players, int index,
            long tick) {
        UUID player = players.get(index);
        Random jitter = new Random(index * 7_919L + tick * 31L);
        long base = tick * TICK_NANOS;
        double[] own = spot(index, tick);
        double[] other = spot(index ^ 1, tick);
        float yaw = (float) (Math.toDegrees(Math.atan2(-(other[0] - own[0]), other[1] - own[1]))
                + jitter.nextGaussian() * 3.0);
        float pitch = (float) (8.0 + jitter.nextGaussian() * 2.0);
        boolean airborne = tick % 5 == 0;
        if (jitter.nextInt(4) != 0) {
            long moveAt = base + 3_000_000L + index * 500_000L;
            server.at(moveAt);
            input.movement(new MovementPacket(player, moveAt, yaw, pitch, true, own[0], airborne ? 64.4 : 64.0,
                    own[1], true, !airborne, false));
        }
        if ((tick + index) % 6 == 0) {
            long swingAt = base + 20_000_000L;
            server.at(swingAt);
            input.swing(new SwingPacket(player, swingAt, true));
            input.attack(new AttackPacket(player, swingAt, FIRST_ENTITY + (index ^ 1), false));
        }
        if (jitter.nextInt(9) == 0) {
            long airAt = base + 25_000_000L;
            server.at(airAt);
            input.swing(new SwingPacket(player, airAt, true));
        }
        if (jitter.nextInt(40) == 0) {
            server.at(base + 30_000_000L);
            input.slotChanged(player);
        }
        long endAt = base + 45_000_000L;
        server.at(endAt);
        input.clientTickEnd(new TickEndPacket(player, endAt, "V_1_21_4"));
    }

    private static double[] spot(int index, long tick) {
        int pair = index / 2;
        double angle = tick * 0.05 + pair + (index % 2 == 0 ? 0.0 : Math.PI);
        return new double[] {pair * 6.0 + 1.4 * Math.cos(angle), 1.4 * Math.sin(angle)};
    }
}
