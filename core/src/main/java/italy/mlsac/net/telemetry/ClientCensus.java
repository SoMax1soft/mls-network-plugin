// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.telemetry;

import italy.mlsac.net.packet.ClientVersions;
import italy.mlsac.net.player.PlayerDirectory;
import italy.mlsac.net.player.PlayerView;
import italy.mlsac.net.schedule.TaskLane;
import italy.mlsac.net.schedule.TaskScheduler;
import italy.mlsac.net.schedule.TaskTicket;
import italy.mlsac.net.schedule.TaskTiming;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Supplier;

public final class ClientCensus implements Supplier<ClientDistribution> {

    public static final long RESAMPLE_TICKS = 600L;
    static final int REPORTED_ENTRIES = 8;
    static final int BRAND_LENGTH_LIMIT = 24;
    static final String UNNAMED_BRAND = "unknown";

    private final TaskScheduler scheduler;
    private final PlayerDirectory players;
    private volatile ClientVersions versions = ClientVersions.NONE;
    private volatile ClientBrands brands = ClientBrands.NONE;
    private volatile ClientDistribution latest = ClientDistribution.EMPTY;
    private TaskTicket task;

    public ClientCensus(TaskScheduler scheduler, PlayerDirectory players) {
        this.scheduler = scheduler;
        this.players = players;
    }

    public void attach(ClientVersions versions, ClientBrands brands) {
        this.versions = versions == null ? ClientVersions.NONE : versions;
        this.brands = brands == null ? ClientBrands.NONE : brands;
    }

    public void start() {
        task = scheduler.submit(TaskLane.MAIN, TaskTiming.repeating(RESAMPLE_TICKS, RESAMPLE_TICKS), this::resample);
        resample();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        latest = ClientDistribution.EMPTY;
    }

    @Override
    public ClientDistribution get() {
        return latest;
    }

    void resample() {
        latest = sample();
    }

    ClientDistribution sample() {
        Map<String, Integer> protocols = new HashMap<>();
        Map<String, Integer> labels = new HashMap<>();
        for (PlayerView player : players.online()) {
            OptionalInt protocol = versions.protocol(player.id());
            if (protocol.isPresent()) {
                count(protocols, Integer.toString(protocol.getAsInt()));
            }
            brands.brand(player.id()).map(ClientCensus::brandKey).ifPresent(brand -> count(labels, brand));
        }
        return new ClientDistribution(leaders(protocols), leaders(labels));
    }

    private static void count(Map<String, Integer> tally, String key) {
        tally.merge(key, 1, Integer::sum);
    }

    private static Map<String, Integer> leaders(Map<String, Integer> tally) {
        Map<String, Integer> ordered = new LinkedHashMap<>();
        tally.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(REPORTED_ENTRIES)
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return ordered;
    }

    static String brandKey(String raw) {
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        StringBuilder clean = new StringBuilder(BRAND_LENGTH_LIMIT);
        for (int index = 0; index < lower.length() && clean.length() < BRAND_LENGTH_LIMIT; index++) {
            char symbol = lower.charAt(index);
            clean.append(accepted(symbol) ? symbol : '_');
        }
        String key = clean.toString();
        return key.isEmpty() || key.chars().allMatch(symbol -> symbol == '_') ? UNNAMED_BRAND : key;
    }

    private static boolean accepted(char symbol) {
        return (symbol >= 'a' && symbol <= 'z') || (symbol >= '0' && symbol <= '9')
                || symbol == '.' || symbol == '-' || symbol == '_' || symbol == '+' || symbol == ':';
    }
}
