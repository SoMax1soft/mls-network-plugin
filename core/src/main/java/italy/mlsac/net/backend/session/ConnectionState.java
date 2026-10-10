// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.backend.BackendAddress;
import italy.mlsac.net.backend.Endpoint;
import italy.mlsac.net.backend.wire.UltraWire;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConnectionState {

    private final AtomicBoolean pollInFlight = new AtomicBoolean();
    private final AtomicInteger predictsInFlight = new AtomicInteger();

    private boolean connected;
    private String sessionKey;
    private UltraWire wire = UltraWire.PLAIN;
    private Endpoint current = Endpoint.PRIMARY;
    private boolean quotaExceeded;
    private boolean stasis;
    private long serverErrorSince = -1L;
    private int networkFailures;
    private int redialCount;
    private boolean reconnectScheduled;
    private boolean failoverInProgress;
    private boolean stopping;
    private long lastPrimaryProbe;

    public synchronized boolean connected() {
        return connected;
    }

    public synchronized Optional<String> sessionKey() {
        return Optional.ofNullable(sessionKey);
    }

    public synchronized PredictRoute predictRoute() {
        return new PredictRoute(Optional.ofNullable(sessionKey), wire);
    }

    public synchronized Endpoint current() {
        return current;
    }

    public synchronized boolean quotaExceeded() {
        return quotaExceeded;
    }

    public synchronized boolean stasis() {
        return stasis;
    }

    public synchronized boolean stopping() {
        return stopping;
    }

    public synchronized OptionalLong serverErrorSince() {
        return serverErrorSince < 0 ? OptionalLong.empty() : OptionalLong.of(serverErrorSince);
    }

    public synchronized void opened(Endpoint endpoint, String session, UltraWire offered) {
        connected = true;
        current = endpoint;
        sessionKey = session;
        wire = offered;
        networkFailures = 0;
        redialCount = 0;
    }

    public synchronized void switched(Endpoint endpoint, String session, UltraWire offered) {
        current = endpoint;
        sessionKey = session;
        wire = offered;
        networkFailures = 0;
        serverErrorSince = -1L;
    }

    public synchronized boolean forgetSession(PredictRoute rejected) {
        if (wire != rejected.wire() || !rejected.sessionKey().equals(Optional.ofNullable(sessionKey))) {
            return false;
        }
        wire = UltraWire.PLAIN;
        boolean was = connected;
        connected = false;
        return was;
    }

    public synchronized boolean markDisconnected() {
        boolean was = connected;
        connected = false;
        return was;
    }

    public synchronized int noteTransportFault() {
        return ++networkFailures;
    }

    public synchronized void recordAnswer() {
        networkFailures = 0;
    }

    public synchronized int takeReconnectAttempt() {
        return redialCount++;
    }

    public synchronized boolean claimReconnectSlot() {
        if (reconnectScheduled) {
            return false;
        }
        reconnectScheduled = true;
        return true;
    }

    public synchronized void releaseReconnectSlot() {
        reconnectScheduled = false;
    }

    public synchronized boolean enterServerError(long now) {
        boolean wasActive = serverErrorSince >= 0;
        serverErrorSince = now;
        return !wasActive;
    }

    public synchronized boolean clearServerError() {
        boolean wasActive = serverErrorSince >= 0;
        serverErrorSince = -1L;
        return wasActive;
    }

    public synchronized void quotaExceeded(boolean value) {
        quotaExceeded = value;
    }

    public synchronized boolean enterStasis() {
        boolean entered = !stasis;
        stasis = true;
        quotaExceeded = true;
        return entered;
    }

    public synchronized boolean leaveStasis() {
        boolean left = stasis;
        stasis = false;
        quotaExceeded = false;
        return left;
    }

    public synchronized boolean beginFailover() {
        if (failoverInProgress) {
            return false;
        }
        failoverInProgress = true;
        return true;
    }

    public synchronized void endFailover() {
        failoverInProgress = false;
    }

    public synchronized boolean primaryProbeDue(long now, long intervalMillis) {
        if (current != Endpoint.RESERVE || now - lastPrimaryProbe < intervalMillis) {
            return false;
        }
        lastPrimaryProbe = now;
        return true;
    }

    public synchronized void notePrimaryLeft(long now) {
        lastPrimaryProbe = now;
    }

    public synchronized void shutdown() {
        stopping = true;
        connected = false;
        sessionKey = null;
        wire = UltraWire.PLAIN;
        quotaExceeded = false;
        serverErrorSince = -1L;
        stasis = false;
    }

    public boolean claimPoll() {
        return pollInFlight.compareAndSet(false, true);
    }

    public void releasePoll() {
        pollInFlight.set(false);
    }

    public boolean claimPredict(int limit) {
        while (true) {
            int current = predictsInFlight.get();
            if (current >= limit) {
                return false;
            }
            if (predictsInFlight.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    public void releasePredict() {
        predictsInFlight.decrementAndGet();
    }

    public synchronized BackendStatus snapshot(BackendAddress address) {
        ApiState api;
        if (stasis) {
            api = ApiState.STASIS;
        } else if (connected && !quotaExceeded && serverErrorSince < 0) {
            api = ApiState.CONNECTED;
        } else if (quotaExceeded) {
            api = ApiState.RATE_LIMITED;
        } else if (serverErrorSince >= 0) {
            api = ApiState.BACKEND_FAULT;
        } else {
            api = ApiState.DISCONNECTED;
        }
        return new BackendStatus(api, Optional.of(address.base(current)), current, connected, stasis);
    }
}
