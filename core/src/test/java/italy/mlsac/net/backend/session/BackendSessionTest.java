// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import italy.mlsac.net.backend.BackendVerdict;
import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.PredictOutcome;
import italy.mlsac.net.backend.http.FailureKind;
import italy.mlsac.net.backend.request.AlertReport;
import italy.mlsac.net.backend.request.WindowSubmission;
import italy.mlsac.net.backend.http.HttpReply;
import italy.mlsac.net.console.LogLevel;
import italy.mlsac.net.testkit.ManualScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class BackendSessionTest {

    private static final WindowSubmission WINDOW = new WindowSubmission(UUID.randomUUID(), "Aura_00",
            new byte[] {'M', 'L', 'S', 'U'});

    @Test
    void connectStartsJobsWithSpecDelays() {
        SessionHarness h = new SessionHarness(false);
        h.transport.reply("/api/v1/init", 200, SessionHarness.init());
        h.session.start();
        h.scheduler.runPending();

        assertEquals(ApiState.CONNECTED, h.session.status().state());
        assertEquals(1, h.transport.callsTo("/init").size());
        assertTrue(h.sink.contains("Connected to http://inference.test"));
        h.scheduler.advance(59);
        assertEquals(0, h.transport.callsTo("/events/poll").size());
        h.scheduler.advance(1);
        assertEquals(1, h.transport.callsTo("/events/poll").size());
        h.scheduler.advance(40);
        assertEquals(1, h.transport.callsTo("/heartbeat").size());
        assertEquals(1, h.transport.callsTo("/online").size());
        h.scheduler.advance(600);
        assertEquals(2, h.transport.callsTo("/heartbeat").size());
        assertTrue(h.transport.callsTo("/online").get(0).body().contains("\"sessionId\":\"s-1\""));
    }

    @Test
    void startupRetriesThreeTimesThenReconnectsInTheBackground() {
        SessionHarness h = new SessionHarness(false);
        h.transport.otherwiseFail(FailureKind.CONNECT);
        h.session.start();
        h.scheduler.runPending();
        assertEquals(1, h.transport.callsTo("/init").size());
        h.scheduler.advance(20);
        assertEquals(2, h.transport.callsTo("/init").size());
        h.scheduler.advance(40);
        assertEquals(3, h.transport.callsTo("/init").size());
        h.scheduler.advance(79);
        assertFalse(h.sink.contains("Cannot reach"));
        h.scheduler.advance(1);
        assertTrue(h.sink.contains("Cannot reach http://inference.test"));
        assertEquals(List.of(2001L), h.random.bounds());
        h.scheduler.advance(199);
        assertEquals(3, h.transport.callsTo("/init").size());
        h.scheduler.advance(1);
        assertEquals(4, h.transport.callsTo("/init").size());
        assertEquals(List.of(2001L, 4001L), h.random.bounds());
    }

    @Test
    void serverErrorOnInitTriesTheReserve() {
        SessionHarness h = new SessionHarness(true);
        h.transport.reply(SessionHarness.PRIMARY + "/api/v1/init", 503, "{}");
        h.transport.reply(SessionHarness.RESERVE + "/api/v1/init", 200, SessionHarness.init());
        h.session.start();
        h.scheduler.runPending();
        assertEquals(Endpoint.RESERVE, h.session.status().endpoint());
        assertEquals(ApiState.CONNECTED, h.session.status().state());
        assertTrue(h.sink.contains("attempting http://reserve.test instead"));
    }

    @Test
    void rejectedKeyNeverTriesTheReserve() {
        SessionHarness h = new SessionHarness(true);
        h.transport.reply(SessionHarness.PRIMARY + "/api/v1/init", 401, "{}");
        h.session.start();
        h.scheduler.runPending();
        assertEquals(0, h.transport.callsTo(SessionHarness.RESERVE + "/api/v1/init").size());
        assertEquals(1, h.sink.texts(LogLevel.ERROR).size());
    }

    @Test
    void predictServerErrorEntersTheStateTwiceAndExpiresAfterAMinute() {
        SessionHarness h = connected(false);
        h.transport.reply("/predict-stream", 503, "{}");
        PredictOutcome outcome = h.session.predict(WINDOW, verdict -> { }).join();
        assertInstanceOf(PredictOutcome.Failed.class, outcome);
        assertEquals(2, h.clock.reads("http.error_enter").size());
        assertEquals(List.of(2001L), h.random.bounds());
        assertEquals(ApiState.BACKEND_FAULT, h.session.status().state());
        assertEquals(new PredictOutcome.Refused(PredictOutcome.RefusalReason.BACKEND_FAULT),
                h.session.predict(WINDOW, verdict -> { }).join());
        h.clock.advanceMillis(60_001);
        h.transport.reply("/predict-stream", 200, "{\"model\":\"ultra\",\"probability\":0.5}");
        assertEquals(new PredictOutcome.Completed(1), h.session.predict(WINDOW, verdict -> { }).join());
        assertTrue(h.sink.contains("Server error state expired"));
    }

    @Test
    void threeConsecutiveNetworkFailuresLoseTheConnection() {
        SessionHarness h = connected(false);
        h.transport.otherwiseFail(FailureKind.TIMEOUT);
        h.scheduler.advance(60);
        assertTrue(h.session.status().connected());
        h.scheduler.advance(40);
        assertFalse(h.session.status().connected());
        assertTrue(h.sink.contains("Connection lost: 3 consecutive network failures, last: reportstats"));
        long timers = h.scheduler.active().stream().filter(e -> e.period() > 0).count();
        assertEquals(1, timers);
        assertEquals(List.of(2001L), h.random.bounds());
    }

    @Test
    void onlineNotFoundFallsBackToReportstatsWithTheSameBody() {
        SessionHarness h = connected(false);
        h.transport.reply("/api/v1/online", 404, "{}");
        h.scheduler.advance(100);
        assertEquals(1, h.transport.callsTo("/reportstats").size());
        assertEquals(h.transport.callsTo("/api/v1/online").get(0).body(), h.transport.callsTo("/reportstats").get(0).body());
    }

    @Test
    void quotaEntersStasisAndTheProbeLeavesIt() {
        SessionHarness h = connected(false);
        h.transport.reply("/api/v1/online", 429, "{}");
        h.scheduler.advance(100);
        assertEquals(ApiState.STASIS, h.session.status().state());
        assertInstanceOf(PredictOutcome.Refused.class, h.session.predict(WINDOW, v -> { }).join());
        long heartbeatsBefore = h.transport.callsTo("/heartbeat").size();
        h.scheduler.advance(100);
        assertEquals(heartbeatsBefore + 1, h.transport.callsTo("/heartbeat").size());
        assertEquals(ApiState.CONNECTED, h.session.status().state());
        assertTrue(h.sink.contains("API available again"));
    }

    @Test
    void ndjsonStreamDeliversVerdictsInOrderUntilDone() {
        SessionHarness h = connected(false);
        h.transport.reply("/predict-stream", 200, "{\"type\":\"prediction\",\"probability\":0.12,\"model\":\"pro\"}\n"
                + "{\"type\":\"error\",\"success\":false,\"model\":\"pro\",\"error\":\"x\"}\n\n"
                + "{\"probability\":0.9,\"model\":\"pulse\",\"error\":\"\"}\n{\"type\":\"DONE\"}\nnot json");
        List<BackendVerdict> verdicts = new ArrayList<>();
        assertEquals(new PredictOutcome.Completed(2), h.session.predict(WINDOW, verdicts::add).join());
        assertEquals(List.of(new BackendVerdict("pro", 0.12), new BackendVerdict("pulse", 0.9)), verdicts);
    }

    @Test
    void predictionWithAnErrorStopsTheStream() {
        SessionHarness h = connected(false);
        h.transport.reply("/predict-stream", 200, "{\"probability\":0.3}\n{\"probability\":0.4,\"error\":\"bad\"}\n"
                + "{\"probability\":0.5}");
        List<BackendVerdict> verdicts = new ArrayList<>();
        assertInstanceOf(PredictOutcome.Failed.class, h.session.predict(WINDOW, verdicts::add).join());
        assertEquals(List.of(new BackendVerdict("unknown", 0.3)), verdicts);
    }

    @Test
    void conflictFailsTheRequestButKeepsTheSession() {
        SessionHarness h = connected(false);
        h.transport.reply("/predict-stream", 409, "{\"error\":{\"code\":\"ULTRA_DISABLED\"}}");
        assertInstanceOf(PredictOutcome.Failed.class, h.session.predict(WINDOW, v -> { }).join());
        assertEquals(ApiState.CONNECTED, h.session.status().state());
    }

    @Test
    void windowsOverTheLimitWaitTheirTurnInsteadOfBeingLost() {
        SessionHarness h = connected(false);
        int limit = PredictChannel.MAX_IN_FLIGHT;
        for (int i = 0; i < limit + 3; i++) {
            h.transport.hold("/predict-stream");
        }
        List<CompletableFuture<PredictOutcome>> busy = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            busy.add(h.session.predict(WINDOW, v -> { }));
        }
        CompletableFuture<PredictOutcome> older = h.session.predict(window("Late_01"), v -> { });
        CompletableFuture<PredictOutcome> newer = h.session.predict(window("Late_01"), v -> { });
        CompletableFuture<PredictOutcome> other = h.session.predict(window("Late_02"), v -> { });
        assertEquals(limit, h.transport.callsTo("/predict-stream").size());
        assertEquals(new PredictOutcome.Dropped(), older.join());
        assertFalse(newer.isDone());
        assertFalse(other.isDone());
        h.transport.held().get(0).complete(HttpReply.of(200, "{\"model\":\"ultra\",\"probability\":0.5}"));
        assertEquals(limit + 1, h.transport.callsTo("/predict-stream").size());
        h.transport.held().get(1).complete(HttpReply.of(200, "{\"model\":\"ultra\",\"probability\":0.5}"));
        assertEquals(limit + 2, h.transport.callsTo("/predict-stream").size());
        h.session.disconnect().join();
        assertEquals(new PredictOutcome.Failed("Canceled"), busy.get(limit - 1).join());
    }

    private static WindowSubmission window(String nick) {
        return new WindowSubmission(UUID.nameUUIDFromBytes(nick.getBytes()), nick, new byte[] {'M', 'L', 'S', 'U'});
    }

    @Test
    void eventTransportErrorsAreNotCountedButPollErrorsAre() {
        SessionHarness h = connected(false);
        h.transport.fail("/api/v1/events", FailureKind.CONNECT).fail("/api/v1/events", FailureKind.CONNECT)
                .fail("/api/v1/events", FailureKind.CONNECT);
        for (int i = 0; i < 3; i++) {
            h.session.sendAlertNotice(new AlertReport(UUID.randomUUID(), "p", "ultra", 0.9, 10.0));
        }
        assertTrue(h.session.status().connected());
    }

    @Test
    void heartbeatGatewayErrorSwitchesToTheReserveAndBack() {
        SessionHarness h = connected(true);
        h.transport.reply(SessionHarness.PRIMARY + "/api/v1/heartbeat", 502, "{}");
        h.transport.reply(SessionHarness.RESERVE + "/api/v1/init", 200, "{\"sessionId\":\"reserve-1\"}");
        h.scheduler.advance(100);
        assertEquals(Endpoint.RESERVE, h.session.status().endpoint());
        assertTrue(h.sink.contains("Switched to reserve"));
        h.clock.advanceMillis(5 * 60 * 1000);
        h.transport.reply(SessionHarness.PRIMARY + "/api/v1/init", 200, "{\"sessionId\":\"primary-2\"}");
        h.scheduler.advance(600);
        assertEquals(Endpoint.PRIMARY, h.session.status().endpoint());
        assertTrue(h.sink.contains("Switched back to primary"));
    }

    @Test
    void disconnectStopsEverything() {
        SessionHarness h = connected(false);
        h.session.disconnect().join();
        int before = h.transport.calls().size();
        h.scheduler.advance(5000);
        assertEquals(before, h.transport.calls().size());
        assertTrue(h.scheduler.active().stream().noneMatch(e -> e.lane() == ManualScheduler.Lane.ASYNC && e.period() > 0));
    }

    private static SessionHarness connected(boolean reserve) {
        SessionHarness h = new SessionHarness(reserve);
        h.transport.reply(SessionHarness.PRIMARY + "/api/v1/init", 200, SessionHarness.init());
        h.session.start();
        h.scheduler.runPending();
        return h;
    }
}
