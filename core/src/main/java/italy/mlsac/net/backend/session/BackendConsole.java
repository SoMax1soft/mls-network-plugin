// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.backend.session;

import italy.mlsac.net.clock.Clock;
import italy.mlsac.net.console.LogTopic;
import italy.mlsac.net.console.PluginLog;
import java.util.HashMap;
import java.util.Map;

public final class BackendConsole {

    private static final long CAUSE_WINDOW_MILLIS = 10 * 60 * 1000L;
    private static final LogTopic TOPIC = LogTopic.BACKEND;

    private final PluginLog log;
    private final Clock clock;
    private final Map<String, Long> causeWarnings = new HashMap<>();
    private boolean reconnectAnnounced;

    public BackendConsole(PluginLog log, Clock clock) {
        this.log = log;
        this.clock = clock;
    }

    public PluginLog log() {
        return log;
    }

    public void connecting(String base) {
        log.debug(TOPIC, () -> "Connecting to " + base);
    }

    public void plaintextEndpoint(String base) {
        log.warn(TOPIC, base + " uses plain http:// - predictions, the API key and punish reports can be read "
                + "or changed in transit; use https://");
    }

    public synchronized void connected(String base, boolean reserve, boolean reconnect) {
        reconnectAnnounced = false;
        String where = reserve ? base + " [backup address]" : base;
        log.info(TOPIC, (reconnect ? "Reconnected to " : "Connected to ") + where);
    }

    public void startFailed(String base, String reason) {
        log.warn(TOPIC, "Cannot reach " + base + ": " + reason + ". Retrying in the background.");
    }

    public void authFailure(String reason) {
        log.error(TOPIC, "The backend refused the API key (" + reason + "); fix detection.api-key in config.yml");
    }

    public void tryingReserve(String candidate, String reason, String reserve) {
        log.warn(TOPIC, "No answer from " + candidate + " (" + reason + "), attempting " + reserve + " instead");
    }

    public synchronized void connectionLost(String context) {
        reconnectAnnounced = false;
        log.warn(TOPIC, "Connection lost: 3 consecutive network failures, last: " + context);
    }

    public synchronized void serverErrorEntered(String reason) {
        reconnectAnnounced = false;
        log.warn(TOPIC, "Entering server error state: " + reason);
    }

    public void serverErrorExpired() {
        log.info(TOPIC, "Server error state expired");
    }

    public void switchedToReserve(String reserve, String primary, String reason) {
        log.warn(TOPIC, "Switched to reserve " + reserve + " (" + primary + ": " + reason + ")");
    }

    public void reserveFailed(String reserve, String reason) {
        log.warn(TOPIC, "Backup address " + reserve + " is unavailable as well: " + reason);
    }

    public void backToPrimary(String primary) {
        log.info(TOPIC, "Switched back to primary " + primary);
    }

    public void stasisEntered() {
        log.warn(TOPIC, "Request limit reached - upgrade the tariff; entering stasis");
    }

    public void stasisLeft() {
        log.info(TOPIC, "API available again");
    }

    public synchronized void reconnectScheduled(long seconds, int attemptNumber) {
        String line = "Reconnect #" + attemptNumber + " in " + seconds + " s";
        if (reconnectAnnounced) {
            log.debug(TOPIC, () -> line);
        } else {
            reconnectAnnounced = true;
            log.info(TOPIC, line);
        }
    }

    public void cause(String cause) {
        log.debug(TOPIC, () -> cause);
        long now = clock.wallMillis("log.cause");
        synchronized (causeWarnings) {
            Long last = causeWarnings.get(cause);
            if (last != null && now - last < CAUSE_WINDOW_MILLIS) {
                return;
            }
            if (causeWarnings.size() > 128) {
                causeWarnings.clear();
            }
            causeWarnings.put(cause, now);
        }
        log.warn(TOPIC, cause);
    }

    public void debug(String line) {
        log.debug(TOPIC, () -> line);
    }
}
