// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.pipeline.replay;

import italy.mlsac.net.pipeline.compensation.EntityMove;
import italy.mlsac.net.pipeline.engine.DataPipeline;
import italy.mlsac.net.pipeline.engine.PipelinePorts;
import italy.mlsac.net.pipeline.geometry.Vec3;
import italy.mlsac.net.pipeline.input.AttackPacket;
import italy.mlsac.net.pipeline.input.MovementPacket;
import italy.mlsac.net.pipeline.input.SwingPacket;
import italy.mlsac.net.pipeline.input.TickEndPacket;
import italy.mlsac.net.pipeline.json.JsonNumber;
import italy.mlsac.net.pipeline.json.JsonReader;
import italy.mlsac.net.pipeline.ultra.PredictRequestBody;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

final class ReplayDriver {
    private static final Set<String> COMPARED = Set.of("ng.window", "legacy.window", "file", "comp.ping");

    private final TraceFile trace;
    private final ReplayRecorder recorder;
    private final ReplayMisses misses = new ReplayMisses();
    private final ReplayReads reads;
    private final ReplayOutputs outputs;
    private final DataPipeline pipeline;
    private final ParityReport report;
    private final WindowComparison windows;
    private final Set<TraceEvent> deliveredNested = new HashSet<>();
    private String sessionKey;
    private final Set<Long> initCalls = new HashSet<>();

    ReplayDriver(Path path) {
        this(path, ReplayRecorder.ABSENT);
    }

    ReplayDriver(Path path, ReplayRecorder recorder) {
        this.trace = new TraceFile(path);
        this.recorder = recorder;
        this.reads = new ReplayReads(trace, misses);
        this.outputs = new ReplayOutputs(reads);
        this.pipeline = new DataPipeline(new PipelinePorts(reads, reads, reads, reads, reads, reads, reads, outputs,
                reads, reads, reads, outputs, outputs.legacy(), outputs), recorder.taps());
        this.report = new ParityReport(path.getFileName().toString());
        this.report.misses = misses;
        this.windows = new WindowComparison(report);
        outputs.onPing(this::nestedPing);
    }

    ReplayDriver tampering(UnaryOperator<ProducedOutput> operator) {
        outputs.tamper(operator);
        return this;
    }

    ParityReport run() {
        trace.forEachEvent(this::replay);
        return report;
    }

    private void replay(TraceEvent event) {
        if (report.cutAt >= 0) {
            return;
        }
        if (!recorder.replays(event)) {
            report.cutAt = event.head().sequence();
            return;
        }
        reads.enter(event);
        dispatch(event);
        for (TraceEvent nested : event.nested()) {
            if ("out.ping".equals(nested.name()) && deliveredNested.add(nested)) {
                reads.enter(nested);
                pipeline.pingWritten(nested.head().player(), (int) nested.head().integer("id"));
                reads.leave();
            }
        }
        reads.leave();
        compare(event, outputs.drain());
        observeRequests(event);
    }

    private void nestedPing(UUID viewer, int id) {
        TraceEvent current = reads.currentEvent();
        for (TraceEvent nested : current.nested()) {
            if ("out.ping".equals(nested.name()) && !deliveredNested.contains(nested)
                    && viewer.equals(nested.head().player()) && nested.head().integer("id") == id) {
                deliveredNested.add(nested);
                reads.enter(nested);
                pipeline.pingWritten(viewer, id);
                reads.leave();
                return;
            }
        }
    }

    private void dispatch(TraceEvent event) {
        TraceRecord head = event.head();
        switch (event.name()) {
            case "plugin.enable" -> pipeline.enabled();
            case "plugin.disable" -> pipeline.disabled();
            case "tick" -> pipeline.gameTick(head.integer("tick"));
            case "pkt.flying" -> pipeline.movement(new MovementPacket(head.player(), head.integer("ns"),
                    head.single("yaw"), head.single("pitch"), head.bool("rot"), head.real("x"), head.real("y"),
                    head.real("z"), head.bool("pos"), head.bool("ground"), head.bool("cancel")));
            case "pkt.tick_end" -> pipeline.clientTickEnd(
                    new TickEndPacket(head.player(), head.integer("ns"), head.text("cv")));
            case "pkt.teleport_confirm" -> pipeline.teleportConfirmed(head.player());
            case "pkt.swing" -> pipeline.swing(new SwingPacket(head.player(), head.integer("ns"), head.bool("main")));
            case "pkt.slot" -> pipeline.slotChanged(head.player());
            case "pkt.attack" -> pipeline.attack(new AttackPacket(head.player(), head.integer("ns"),
                    (int) head.integer("e"), head.bool("cancel")));
            case "pkt.pong" -> pipeline.pong(head.player(), (int) head.integer("id"));
            case "out.ping" -> pipeline.pingWritten(head.player(), (int) head.integer("id"));
            case "out.spawn" -> pipeline.entitySpawned(head.player(), (int) head.integer("e"),
                    new Vec3(head.real("x"), head.real("y"), head.real("z")));
            case "out.move" -> pipeline.entityMoved(head.player(), new EntityMove((int) head.integer("e"),
                    head.bool("rel"), head.bool("pos"), head.real("x"), head.real("y"), head.real("z")));
            case "out.destroy" -> pipeline.entitiesDestroyed(head.player(), ids(head));
            case "out.reset" -> pipeline.viewReset(head.player());
            case "player.join" -> pipeline.joined(head.player());
            case "player.quit" -> pipeline.quit(head.player());
            case "player.death" -> pipeline.died(head.player());
            case "player.respawn" -> pipeline.respawned(head.player());
            case "player.world" -> pipeline.worldSwitched(head.player());
            case "player.teleport" -> pipeline.teleported(head.player());
            case "cmd.record_start" -> recorder.start(head.player(), head.text("label"), head.text("comment"));
            case "cmd.record_stop" -> recorder.stop(head.player());
            case "cmd.record_stop_all" -> recorder.stopAll();
            default -> {
            }
        }
    }

    private static int[] ids(TraceRecord head) {
        List<Object> values = head.list("ids");
        int[] ids = new int[values.size()];
        for (int index = 0; index < ids.length; index++) {
            ids[index] = ((JsonNumber) values.get(index)).asInt();
        }
        return ids;
    }

    private void compare(TraceEvent event, List<ProducedOutput> produced) {
        checkOrder(event, produced);
        for (String kind : COMPARED) {
            List<TraceRecord> expected = new ArrayList<>();
            for (TraceRecord output : event.outputs()) {
                if (kind.equals(output.name()) && recorder.expects(output)) {
                    expected.add(output);
                }
            }
            List<ProducedOutput> actual = new ArrayList<>();
            for (ProducedOutput output : produced) {
                if (kind.equals(output.kind())) {
                    actual.add(output);
                }
            }
            if (expected.isEmpty() && actual.isEmpty()) {
                continue;
            }
            KindTally tally = report.kind(kind);
            tally.expected += expected.size();
            tally.produced += actual.size();
            for (int index = 0; index < Math.max(expected.size(), actual.size()); index++) {
                TraceRecord want = index < expected.size() ? expected.get(index) : null;
                ProducedOutput got = index < actual.size() ? actual.get(index) : null;
                compareOne(kind, event, want, got, tally);
            }
        }
    }

    private void checkOrder(TraceEvent event, List<ProducedOutput> produced) {
        List<String> expected = new ArrayList<>();
        for (TraceRecord output : event.outputs()) {
            if (COMPARED.contains(output.name()) && recorder.expects(output)) {
                expected.add(output.name() + "|" + identity(output.name(), output));
            }
        }
        List<String> actual = new ArrayList<>();
        for (ProducedOutput output : produced) {
            actual.add(output.kind() + "|" + output.identity());
        }
        if (!expected.isEmpty() || !actual.isEmpty()) {
            report.orderedEvents++;
            if (expected.equals(actual)) {
                report.orderedEventsMatching++;
            } else if (report.firstOrderDifference < 0) {
                report.firstOrderDifference = event.head().sequence();
            }
        }
    }

    private void compareOne(String kind, TraceEvent event, TraceRecord want, ProducedOutput got, KindTally tally) {
        long sequence = want != null ? want.sequence() : event.head().sequence();
        if (want == null || got == null) {
            tally.mismatch(sequence, want == null ? "unexpected " + kind + " " + got.identity()
                    : "missing " + kind + " " + identity(kind, want));
            return;
        }
        byte[] expectedBytes = bytes(kind, want);
        boolean sameIdentity = identity(kind, want).equals(got.identity());
        if ("file".equals(kind)) {
            String verdict = FileDiff.describe(got.identity(), expectedBytes, got.bytes());
            report.files.add(want.text("path") + " : " + (sameIdentity ? verdict : "path differs: " + got.identity()));
            if (sameIdentity && FileDiff.equivalent(got.identity(), expectedBytes, got.bytes())) {
                tally.matched++;
            } else {
                tally.mismatch(sequence, want.text("path"));
            }
            return;
        }
        if (sameIdentity && Arrays.equals(expectedBytes, got.bytes())) {
            tally.matched++;
            return;
        }
        if ("ng.window".equals(kind) && sameIdentity) {
            windows.compare(expectedBytes, got.bytes());
        }
        tally.mismatch(sequence, sameIdentity ? "bytes differ" : "identity " + identity(kind, want) + " vs "
                + got.identity());
    }

    private static String identity(String kind, TraceRecord record) {
        return "file".equals(kind) ? record.text("path") : record.text("p");
    }

    private static byte[] bytes(String kind, TraceRecord record) {
        return switch (kind) {
            case "comp.ping" -> Long.toString(record.integer("id")).getBytes(StandardCharsets.US_ASCII);
            case "file" -> record.has("content")
                    ? record.text("content").getBytes(StandardCharsets.UTF_8)
                    : Base64.getDecoder().decode(record.text("content64"));
            default -> Base64.getDecoder().decode(record.text("b64"));
        };
    }

    private void observeRequests(TraceEvent event) {
        if ("plugin.config".equals(event.name()) && "reload".equals(event.head().text("why"))) {
            sessionKey = null;
        }
        for (TraceRecord record : event.records()) {
            if (record.isOutput() && "http".equals(record.name()) && record.text("url").endsWith("/api/v1/init")) {
                initCalls.add(record.integer("co"));
            }
            if (record.isOutput() && "http".equals(record.name())
                    && record.text("url").endsWith("/api/v1/predict-stream")) {
                checkPredict(record);
            }
        }
        TraceRecord head = event.head();
        if ("http.done".equals(event.name()) && initCalls.contains(head.integer("co")) && head.has("body")) {
            Map<String, Object> body = JsonReader.parseObject(head.text("body"));
            Object data = body.get("data");
            if (data instanceof Map<?, ?> map && map.get("sessionId") instanceof String id) {
                sessionKey = id;
            }
        }
    }

    private void checkPredict(TraceRecord request) {
        report.predictBodies++;
        String body = request.text("body");
        Map<String, Object> fields = JsonReader.parseObject(body);
        UUID player = UUID.fromString((String) fields.get("playerUuid"));
        byte[] last = outputs.lastWindow(player);
        byte[] sent = Base64.getDecoder().decode((String) fields.get("playerData"));
        if (last != null && Arrays.equals(last, sent)) {
            report.predictPayloadsMatched++;
        }
        Map<String, Object> config = reads.config();
        String ip = trace.header().text("server_ip");
        PredictRequestBody rebuilt = new PredictRequestBody(trace.header().text("plugin_version"),
                (String) config.get("server-identity.name"), (String) config.get("server-identity.family"),
                ip == null || ip.isEmpty() ? "unknown" : ip, (int) trace.header().integer("server_port"),
                Boolean.TRUE.equals(config.get("server-identity.interserver.enabled")),
                (String) config.get("remote-config.preset"), sessionKey);
        String rendered = rebuilt.render(sent, player, (String) fields.get("playerName"));
        if (rendered.equals(body)) {
            report.predictBodiesMatched++;
        } else {
            report.notes.add("predict body differs s=" + request.sequence() + ": " + body.replaceAll("playerData\":\"[^\"]*", "playerData...") + " vs " + rendered.replaceAll("playerData\":\"[^\"]*", "playerData..."));
        }
        if (outputs.everEmitted(player, sent)) {
            report.predictPayloadsEmitted++;
        }
    }
}
