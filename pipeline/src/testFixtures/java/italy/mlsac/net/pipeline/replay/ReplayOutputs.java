// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import italy.mlsac.net.backend.wire.UltraPayload;
import italy.mlsac.net.pipeline.environment.DataFolder;
import italy.mlsac.net.pipeline.environment.LegacyWindowSink;
import italy.mlsac.net.pipeline.environment.TransactionPings;
import italy.mlsac.net.pipeline.environment.UltraWindowSink;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

final class ReplayOutputs implements UltraWindowSink, TransactionPings, DataFolder {
    private final ReplayReads reads;
    private final List<ProducedOutput> produced = new ArrayList<>();
    private final Map<UUID, byte[]> lastWindows = new HashMap<>();
    private final Map<UUID, List<byte[]>> allWindows = new HashMap<>();
    private BiConsumer<UUID, Integer> pingListener = (viewer, id) -> { };
    private UnaryOperator<ProducedOutput> tamper = UnaryOperator.identity();

    ReplayOutputs(ReplayReads reads) {
        this.reads = reads;
    }

    void tamper(UnaryOperator<ProducedOutput> operator) {
        this.tamper = operator;
    }

    private void record(ProducedOutput output) {
        produced.add(tamper.apply(output));
    }

    void onPing(BiConsumer<UUID, Integer> listener) {
        this.pingListener = listener;
    }

    List<ProducedOutput> drain() {
        List<ProducedOutput> copy = new ArrayList<>(produced);
        produced.clear();
        return copy;
    }

    boolean everEmitted(UUID player, byte[] window) {
        for (byte[] emitted : allWindows.getOrDefault(player, List.of())) {
            if (Arrays.equals(emitted, window)) {
                return true;
            }
        }
        return false;
    }

    byte[] lastWindow(UUID player) {
        return lastWindows.get(player);
    }

    @Override
    public void accept(UUID player, byte[] produced) {
        byte[] window = UltraPayload.read(produced).filter(UltraPayload::wide).map(UltraPayload::layoutFour)
                .orElse(produced);
        record(new ProducedOutput("ng.window", player.toString(), window));
        lastWindows.put(player, window);
        allWindows.computeIfAbsent(player, ignored -> new ArrayList<>()).add(window);
    }

    LegacyWindowSink legacy() {
        return (player, window) -> {
            record(new ProducedOutput("legacy.window", player.toString(), window));
            lastWindows.put(player, window);
            allWindows.computeIfAbsent(player, ignored -> new ArrayList<>()).add(window);
        };
    }

    @Override
    public void send(UUID viewer, int transactionId) {
        record(new ProducedOutput("comp.ping", viewer.toString(),
                Integer.toString(transactionId).getBytes(StandardCharsets.US_ASCII)));
        pingListener.accept(viewer, transactionId);
    }

    @Override
    public boolean exists(String relativePath) {
        return reads.fileExists(relativePath);
    }

    @Override
    public void write(String relativePath, byte[] content) {
        record(new ProducedOutput("file", relativePath, content));
    }
}
