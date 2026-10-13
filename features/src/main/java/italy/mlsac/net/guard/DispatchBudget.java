// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.guard;

import italy.mlsac.net.config.guard.CommandGuardSettings;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class DispatchBudget {

    static final long WINDOW_MILLIS = 60_000L;
    private static final long MINUTE_MILLIS = 60_000L;

    private final Deque<Long> overall = new ArrayDeque<>();
    private final Map<String, Deque<Long>> perPlayer = new HashMap<>();
    private final Map<String, Deque<Spend>> perRoot = new HashMap<>();
    private long pausedUntil = Long.MIN_VALUE;

    public synchronized Optional<GuardRefusal> check(String rootKey, Optional<PunishedPlayer> target, long now,
                                                     CommandGuardSettings limits) {
        forgetBefore(now - WINDOW_MILLIS);
        if (now < pausedUntil) {
            return Optional.of(GuardRefusal.of(GuardReason.PAUSED));
        }
        if (overall.size() >= limits.maxCommandsPerMinute()) {
            return Optional.of(new GuardRefusal(GuardReason.RATE_LIMIT,
                    String.valueOf(limits.maxCommandsPerMinute())));
        }
        if (target.isEmpty()) {
            return Optional.empty();
        }
        String player = target.get().id().toString();
        if (perPlayer.getOrDefault(player, new ArrayDeque<>()).size() >= limits.maxCommandsPerPlayerPerMinute()) {
            return Optional.of(new GuardRefusal(GuardReason.PLAYER_RATE_LIMIT,
                    String.valueOf(limits.maxCommandsPerPlayerPerMinute())));
        }
        int players = playersWith(rootKey, player);
        if (players > limits.maxSameCommandBurst()) {
            pausedUntil = now + limits.burstPauseMinutes() * MINUTE_MILLIS;
            return Optional.of(new GuardRefusal(GuardReason.BURST, String.valueOf(players)));
        }
        return Optional.empty();
    }

    public synchronized void record(String rootKey, Optional<PunishedPlayer> target, long now) {
        overall.addLast(now);
        target.ifPresent(player -> {
            String id = player.id().toString();
            perPlayer.computeIfAbsent(id, key -> new ArrayDeque<>()).addLast(now);
            perRoot.computeIfAbsent(rootKey, key -> new ArrayDeque<>()).addLast(new Spend(now, id));
        });
    }

    public synchronized boolean paused(long now) {
        return now < pausedUntil;
    }

    private int playersWith(String rootKey, String player) {
        Set<String> players = new HashSet<>();
        perRoot.getOrDefault(rootKey, new ArrayDeque<>()).forEach(spend -> players.add(spend.player()));
        players.add(player);
        return players.size();
    }

    private void forgetBefore(long oldest) {
        while (!overall.isEmpty() && overall.peekFirst() < oldest) {
            overall.pollFirst();
        }
        perPlayer.values().forEach(times -> {
            while (!times.isEmpty() && times.peekFirst() < oldest) {
                times.pollFirst();
            }
        });
        perPlayer.values().removeIf(Deque::isEmpty);
        perRoot.values().forEach(spends -> {
            while (!spends.isEmpty() && spends.peekFirst().at() < oldest) {
                spends.pollFirst();
            }
        });
        perRoot.values().removeIf(Deque::isEmpty);
    }

    private record Spend(long at, String player) {
    }
}
