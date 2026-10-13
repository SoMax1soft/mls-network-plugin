// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.console;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class DebugTally {

    private static final ConcurrentHashMap<String, LongAdder> COUNTS = new ConcurrentHashMap<>();
    private static volatile boolean counting;
    private static volatile Boolean forced;
    private static volatile UUID traced;
    private static volatile String tracedName;
    private static volatile Consumer<String> traceSink = line -> { };

    private DebugTally() {
    }

    public static void count(String key) {
        if (counting) {
            COUNTS.computeIfAbsent(key, k -> new LongAdder()).increment();
        }
    }

    public static void count(String key, long amount) {
        if (counting) {
            COUNTS.computeIfAbsent(key, k -> new LongAdder()).add(amount);
        }
    }

    public static boolean counting() {
        return counting;
    }

    public static void counting(boolean on) {
        counting = on;
        if (!on) {
            COUNTS.clear();
        }
    }

    public static Boolean forced() {
        return forced;
    }

    public static void force(Boolean value) {
        forced = value;
    }

    public static boolean on(boolean configured) {
        Boolean f = forced;
        return f != null ? f : configured;
    }

    public static Map<String, Long> drain() {
        Map<String, Long> out = new TreeMap<>();
        for (Map.Entry<String, LongAdder> e : COUNTS.entrySet()) {
            long v = e.getValue().sumThenReset();
            if (v != 0) {
                out.put(e.getKey(), v);
            }
        }
        return out;
    }

    public static void traceSink(Consumer<String> sink) {
        traceSink = sink == null ? line -> { } : sink;
    }

    public static void traceTarget(UUID player, String name) {
        traced = player;
        tracedName = name;
    }

    public static UUID traced() {
        return traced;
    }

    public static String tracedName() {
        return tracedName;
    }

    public static boolean tracing(UUID player) {
        UUID t = traced;
        return t != null && t.equals(player);
    }

    public static void trace(UUID player, Supplier<String> line) {
        UUID t = traced;
        if (t != null && t.equals(player)) {
            traceSink.accept("[" + (tracedName == null ? player : tracedName) + "] " + line.get());
        }
    }
}
