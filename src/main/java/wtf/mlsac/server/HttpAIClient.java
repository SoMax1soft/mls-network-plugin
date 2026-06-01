/*
 * MLSAC is a GPLv3 licensed fork of a Minecraft anti-cheat system.
 * This project is community-maintained and not affiliated with any single upstream repository.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This file is based on GPLv3 licensed work and includes modifications.
 * Derived from:
 *   - SlothAC (© 2025 KaelusMC, https://github.com/KaelusMC/SlothAC)
 *   - Grim (© 2025 GrimAnticheat, https://github.com/GrimAnticheat/Grim)
 *   - Client-side project (GPLv3: https://github.com/MLSAC/client-side)
 */

package wtf.mlsac.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.mlsac.Main;
import wtf.mlsac.Permissions;
import wtf.mlsac.alert.AlertManager;
import wtf.mlsac.scheduler.SchedulerManager;
import wtf.mlsac.scheduler.ScheduledTask;
import wtf.mlsac.util.ColorUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpAIClient implements IAIClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final long HEARTBEAT_INTERVAL_MS = 30000;
    private static final long REPORT_STATS_INTERVAL_MS = 30000;
    private static final long INTERSERVER_EVENT_POLL_INTERVAL_MS = 3000;
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int READ_TIMEOUT_SECONDS = 30;
    private static final int WRITE_TIMEOUT_SECONDS = 30;
    private static final long PERIODIC_CHECK_INTERVAL_MS = 60000;
    private static final long STASIS_CHECK_INTERVAL_MS = 300000; // 5 minutes
    private static final int DUPLICATE_NAME_WARNING_LIMIT = 3;
    private static final long DUPLICATE_NAME_WARNING_INTERVAL_MS = 30000;
    private static final int INTERSERVER_EVENT_CACHE_LIMIT = 512;

    private final JavaPlugin plugin;
    private final String serverAddress;
    private final String apiKey;
    private final Logger logger;
    private final IntSupplier onlinePlayersSupplier;
    private final boolean debug;
    private final String serverName;
    private final String serverFamily;
    private final boolean interServerEnabled;
    private final boolean eventReportingEnabled;
    private final double apiAlertEventThreshold;
    private final ExecutorService httpExecutor;
    private final OkHttpClient httpClient;
    private final AtomicReference<ScheduledTask> heartbeatTask = new AtomicReference<>();
    private final AtomicReference<ScheduledTask> reportStatsTask = new AtomicReference<>();
    private final AtomicReference<ScheduledTask> interserverEventTask = new AtomicReference<>();
    private final AtomicReference<ScheduledTask> periodicCheckTask = new AtomicReference<>();
    private final AtomicReference<ScheduledTask> reconnectTask = new AtomicReference<>();
    private final AtomicReference<ScheduledTask> stasisCheckTask = new AtomicReference<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean interserverPollInFlight = new AtomicBoolean(false);
    private final AtomicBoolean inStasisMode = new AtomicBoolean(false);
    private volatile boolean autoReconnectEnabled = true;
    private volatile String sessionId = null;
    private volatile boolean limitExceeded = false;
    private volatile boolean serverErrorState = false;
    private volatile long lastServerErrorTime = 0;
    private volatile long lastDuplicateNameWarningTime = 0;
    private final AtomicInteger duplicateNameWarningsRemaining = new AtomicInteger(DUPLICATE_NAME_WARNING_LIMIT);
    private final Set<String> seenInterserverEventIds = ConcurrentHashMap.newKeySet();
    private final Queue<String> seenInterserverEventOrder = new ConcurrentLinkedQueue<>();
    private static final long SERVER_ERROR_SILENCE_MS = 60000;

    public HttpAIClient(JavaPlugin plugin, String serverAddress, String apiKey,
                        IntSupplier onlinePlayersSupplier, boolean debug) {
        this(plugin, serverAddress, apiKey, onlinePlayersSupplier, debug,
                "default", "default", false, true, 0.75);
    }

    public HttpAIClient(JavaPlugin plugin, String serverAddress, String apiKey,
                        IntSupplier onlinePlayersSupplier, boolean debug,
                        String serverName, String serverFamily, boolean interServerEnabled,
                        boolean eventReportingEnabled, double apiAlertEventThreshold) {
        this.plugin = plugin;
        this.serverAddress = serverAddress;
        this.apiKey = apiKey;
        this.logger = plugin.getLogger();
        this.onlinePlayersSupplier = onlinePlayersSupplier;
        this.debug = debug;
        this.serverName = serverName != null && !serverName.trim().isEmpty()
                ? serverName.trim()
                : "default";
        this.serverFamily = serverFamily != null && !serverFamily.trim().isEmpty()
                ? serverFamily.trim()
                : "default";
        this.interServerEnabled = interServerEnabled;
        this.eventReportingEnabled = eventReportingEnabled;
        this.apiAlertEventThreshold = apiAlertEventThreshold;
        int workers = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));
        this.httpExecutor = Executors.newFixedThreadPool(workers, r -> {
            Thread thread = new Thread(r, "http-ai-client-worker");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
    }

    public Executor getExecutor() {
        return httpExecutor;
    }

    private String getAdvertisedServerIp() {
        String bukkitIp = plugin.getServer().getIp();
        return bukkitIp != null && !bukkitIp.trim().isEmpty() ? bukkitIp.trim() : "unknown";
    }

    private int getAdvertisedServerPort() {
        return plugin.getServer().getPort();
    }

    private JsonObject createBasePayload() {
        JsonObject json = new JsonObject();
        json.addProperty("pluginVersion", plugin.getDescription().getVersion());
        json.addProperty("interserverEventsSupported", true);
        json.addProperty("serverName", serverName);
        json.addProperty("serverFamily", serverFamily);
        json.addProperty("family", serverFamily);
        json.addProperty("serverIp", getAdvertisedServerIp());
        json.addProperty("serverPort", getAdvertisedServerPort());
        json.addProperty("interServer", interServerEnabled);
        return json;
    }

    private RequestBody jsonBody(JsonObject json) {
        return RequestBody.create(JSON, json.toString());
    }

    private void addOnline(JsonObject json) {
        int online = onlinePlayersSupplier.getAsInt();
        json.addProperty("onlinePlayers", online);
        json.addProperty("onlineCount", online);
    }

    private void addSession(JsonObject json) {
        if (sessionId != null) {
            json.addProperty("sessionId", sessionId);
        }
    }

    private void handleApiWarnings(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return;
        }
        try {
            JsonObject root = new JsonParser().parse(responseBody).getAsJsonObject();
            if (!root.has("warnings") || !root.get("warnings").isJsonObject()) {
                return;
            }
            JsonObject warnings = root.getAsJsonObject("warnings");
            if (!warnings.has("duplicateServerName") || !warnings.get("duplicateServerName").isJsonObject()) {
                return;
            }
            JsonObject duplicate = warnings.getAsJsonObject("duplicateServerName");
            if (!duplicate.has("active") || !duplicate.get("active").getAsBoolean()) {
                duplicateNameWarningsRemaining.set(DUPLICATE_NAME_WARNING_LIMIT);
                return;
            }
            String message = duplicate.has("message")
                    ? duplicate.get("message").getAsString()
                    : "MLSAC server-name is duplicated on another active server. Change server-identity.name in config.yml.";
            warnDuplicateServerName(message);
        } catch (Exception ignored) {
        }
    }

    private void handleInterserverEvents(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return;
        }

        try {
            JsonObject root = new JsonParser().parse(responseBody).getAsJsonObject();
            if (!root.has("data") || !root.get("data").isJsonObject()) {
                return;
            }

            JsonObject data = root.getAsJsonObject("data");
            if (!data.has("interserverEvents") || !data.get("interserverEvents").isJsonArray()) {
                return;
            }

            JsonArray events = data.getAsJsonArray("interserverEvents");
            for (JsonElement element : events) {
                if (element != null && element.isJsonObject()) {
                    handleInterserverEvent(element.getAsJsonObject());
                }
            }
        } catch (Exception e) {
            if (debug) {
                logger.warning("[HTTP] Failed to parse inter-server events: " + e.getMessage());
            }
        }
    }

    private void handleInterserverEvent(JsonObject event) {
        String eventId = getJsonString(event, "id", "");
        if (!rememberInterserverEvent(eventId)) {
            return;
        }
        if (!(plugin instanceof Main)) {
            return;
        }

        AlertManager alertManager = ((Main) plugin).getAlertManager();
        if (alertManager == null) {
            return;
        }

        String type = getJsonString(event, "type", "alert");
        String sourceServerName = getJsonString(event, "serverName", "unknown");
        String playerName = getJsonString(event, "playerName", "Unknown");
        String model = getJsonString(event, "model", "unknown");
        String action = getJsonString(event, "action", type);
        double probability = getJsonDouble(event, "probability", 0.0);
        double buffer = getJsonDouble(event, "buffer", 0.0);
        int violationLevel = (int) Math.round(getJsonDouble(event, "violationLevel", getJsonDouble(event, "vl", 0.0)));

        alertManager.sendInterServerEvent(type, sourceServerName, playerName, probability,
                buffer, violationLevel, model, action);
    }

    private boolean rememberInterserverEvent(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return true;
        }
        if (!seenInterserverEventIds.add(eventId)) {
            return false;
        }

        seenInterserverEventOrder.add(eventId);
        while (seenInterserverEventOrder.size() > INTERSERVER_EVENT_CACHE_LIMIT) {
            String oldEventId = seenInterserverEventOrder.poll();
            if (oldEventId == null) {
                break;
            }
            seenInterserverEventIds.remove(oldEventId);
        }
        return true;
    }

    private String getJsonString(JsonObject object, String key, String fallback) {
        try {
            if (object.has(key) && !object.get(key).isJsonNull()) {
                return object.get(key).getAsString();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private double getJsonDouble(JsonObject object, String key, double fallback) {
        try {
            if (object.has(key) && !object.get(key).isJsonNull()) {
                return object.get(key).getAsDouble();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private void warnDuplicateServerName(String message) {
        long now = System.currentTimeMillis();
        if (duplicateNameWarningsRemaining.get() <= 0) {
            return;
        }
        if (now - lastDuplicateNameWarningTime < DUPLICATE_NAME_WARNING_INTERVAL_MS) {
            return;
        }
        lastDuplicateNameWarningTime = now;
        duplicateNameWarningsRemaining.decrementAndGet();

        logger.warning("[MLSAC] " + message);
        SchedulerManager.getAdapter().runSync(() -> {
            String chatMessage = ColorUtil.colorize("&c[MLSAC] &f" + message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(Permissions.ALERTS) || player.isOp()) {
                    player.sendMessage(chatMessage);
                }
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        if (shuttingDown.get()) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("[HTTP] Connecting to " + serverAddress + "...");

                String initUrl = serverAddress + "/api/v1/init";
                JsonObject initJson = createBasePayload();
                initJson.addProperty("apiKey", apiKey);
                addOnline(initJson);
                RequestBody initBody = jsonBody(initJson);
                Request initRequest = new Request.Builder()
                        .url(initUrl)
                        .post(initBody)
                        .build();

                try (Response response = httpClient.newCall(initRequest).execute()) {
                    if (response.code() == 401 || response.code() == 403) {
                        logger.severe("[HTTP] Authentication failed! API key is invalid, expired, or corrupted. Please check your API key in config.yml");
                        connected.set(false);
                        return false;
                    }
                    if (!response.isSuccessful()) {
                        logger.warning("[HTTP] Init failed: HTTP " + response.code());
                        connected.set(false);
                        return false;
                    }

                    ResponseBody body = response.body();
                    String responseBody;
                    if (body != null) {
                        responseBody = body.string();
                    } else {
                        responseBody = "";
                    }
                    handleApiWarnings(responseBody);
                    sessionId = extractSessionId(responseBody);
                    if (sessionId == null || sessionId.isEmpty()) {
                        sessionId = "http-session-" + System.currentTimeMillis();
                    }
                }

                connected.set(true);
                logger.info("[HTTP] Connected successfully. Session: " + sessionId);

                startHeartbeat();
                startReportStats();
                startInterserverEventPoll();
                startPeriodicCheck();

                return true;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[HTTP] Connection failed: " + e.getMessage());
                connected.set(false);
                return false;
            }
        }, httpExecutor);
    }

    private String extractSessionId(String responseBody) {
        try {
            if (responseBody.contains("sessionId")) {
                int start = responseBody.indexOf("sessionId") + 12;
                int end = responseBody.indexOf("\"", start);
                if (end > start) {
                    return responseBody.substring(start, end);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public CompletableFuture<Boolean> connectWithRetry() {
        return connectWithRetry(0);
    }

    private CompletableFuture<Boolean> connectWithRetry(int attempt) {
        if (shuttingDown.get()) {
            return CompletableFuture.completedFuture(false);
        }
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            logger.severe("[HTTP] Max retry attempts reached");
            return CompletableFuture.completedFuture(false);
        }
        return connect().thenCompose(success -> {
            if (success) {
                return CompletableFuture.completedFuture(true);
            }
            long backoffMs = INITIAL_BACKOFF_MS * (1L << attempt);
            logger.info("[HTTP] Retrying in " + backoffMs + "ms (attempt " + (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS + ")");
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            SchedulerManager.getAdapter().runAsyncDelayed(() -> {
                connectWithRetry(attempt + 1).thenAccept(future::complete);
            }, backoffMs / 50);
            return future;
        });
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        shuttingDown.set(true);
        autoReconnectEnabled = false;

        ScheduledTask hb = heartbeatTask.getAndSet(null);
        if (hb != null) hb.cancel();
        ScheduledTask rs = reportStatsTask.getAndSet(null);
        if (rs != null) rs.cancel();
        ScheduledTask ie = interserverEventTask.getAndSet(null);
        if (ie != null) ie.cancel();
        ScheduledTask pc = periodicCheckTask.getAndSet(null);
        if (pc != null) pc.cancel();
        ScheduledTask rt = reconnectTask.getAndSet(null);
        if (rt != null) rt.cancel();
        ScheduledTask st = stasisCheckTask.getAndSet(null);
        if (st != null) st.cancel();

        connected.set(false);
        sessionId = null;
        limitExceeded = false;
        serverErrorState = false;
        inStasisMode.set(false);

        return CompletableFuture.runAsync(() -> {
            logger.info("[HTTP] Disconnected from server");
            httpClient.dispatcher().cancelAll();
            httpClient.connectionPool().evictAll();
            httpExecutor.shutdown();
            try {
                if (!httpExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    httpExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                httpExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }).thenApply(v -> null);
    }

    private void startHeartbeat() {
        ScheduledTask existing = heartbeatTask.get();
        if (existing != null) existing.cancel();

        heartbeatTask.set(SchedulerManager.getAdapter().runAsyncRepeating(() -> {
            if (shuttingDown.get() || !autoReconnectEnabled) return;
            sendHeartbeat();
        }, 100, HEARTBEAT_INTERVAL_MS / 50));
    }

    private void sendHeartbeat() {
        CompletableFuture.runAsync(() -> {
            try {
                String url = serverAddress + "/api/v1/heartbeat";
                JsonObject json = createBasePayload();
                addSession(json);
                addOnline(json);

                RequestBody body = jsonBody(json);
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header("X-API-Key", apiKey)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    ResponseBody respBody = response.body();
                    String responseBody = respBody != null ? respBody.string() : "";
                    handleApiWarnings(responseBody);
                    int code = response.code();
                    if (!response.isSuccessful()) {
                        if (debug) logger.warning("[HTTP] Heartbeat failed: " + code);
                        if (code == 401 || code == 403) {
                            logger.severe("[HTTP] Authentication failed! API key is invalid, expired, or corrupted. Please check your API key in config.yml");
                            scheduleReconnect();
                        } else if (code >= 500) {
                            logger.warning("[HTTP] Heartbeat received server error " + code);
                            enterServerErrorState("Heartbeat received HTTP " + code);
                        }
                    }
                }
            } catch (IOException e) {
                if (debug) logger.warning("[HTTP] Heartbeat error: " + e.getMessage());
            }
        }, httpExecutor);
    }

    private void startReportStats() {
        ScheduledTask existing = reportStatsTask.getAndSet(null);
        if (existing != null) existing.cancel();

        reportStatsTask.set(SchedulerManager.getAdapter().runAsyncRepeating(() -> {
            if (shuttingDown.get() || !autoReconnectEnabled) return;
            sendReportStats();
        }, 100, REPORT_STATS_INTERVAL_MS / 50));
    }

    private void startInterserverEventPoll() {
        ScheduledTask existing = interserverEventTask.getAndSet(null);
        if (existing != null) existing.cancel();

        interserverEventTask.set(SchedulerManager.getAdapter().runAsyncRepeating(() -> {
            if (shuttingDown.get() || !autoReconnectEnabled) return;
            sendInterserverEventPoll();
        }, 60, INTERSERVER_EVENT_POLL_INTERVAL_MS / 50));
    }

    private void startPeriodicCheck() {
        ScheduledTask existing = periodicCheckTask.get();
        if (existing != null) existing.cancel();

        periodicCheckTask.set(SchedulerManager.getAdapter().runAsyncRepeating(() -> {
            if (shuttingDown.get() || !autoReconnectEnabled) return;
            performPeriodicCheck();
        }, 100, PERIODIC_CHECK_INTERVAL_MS / 50));
    }

    private void performPeriodicCheck() {
        if (debug) logger.info("[HTTP] Periodic check running...");
        if (isServerInErrorState()) {
            if (debug) logger.info("[HTTP] Still in server error state, attempting to reconnect...");
            scheduleReconnect();
            return;
        }
        if (limitExceeded) {
            if (debug) logger.info("[HTTP] Limit exceeded, will retry after timeout");
            return;
        }
        if (!connected.get()) {
            if (debug) logger.info("[HTTP] Not connected, attempting to reconnect...");
            scheduleReconnect();
        }
    }

    private void sendReportStats() {
        CompletableFuture.runAsync(() -> {
            try {
                String url = serverAddress + "/api/v1/online";
                JsonObject json = createBasePayload();
                addSession(json);
                addOnline(json);

                RequestBody body = jsonBody(json);
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header("X-API-Key", apiKey)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    int code = response.code();
                    ResponseBody respBody = response.body();
                    String responseBody = respBody != null ? respBody.string() : "";
                    handleApiWarnings(responseBody);
                    if (code == 404 || code == 405) {
                        sendLegacyReportStats(json);
                        return;
                    }
                    if (response.isSuccessful()) {
                        handleInterserverEvents(responseBody);
                        limitExceeded = false;
                        serverErrorState = false;
                        exitStasisMode();
                    } else if (code == 429) {
                        handleRateLimitError();
                    } else if (code >= 500) {
                        logger.warning("[HTTP] ReportStats received server error " + code);
                        enterServerErrorState("ReportStats received HTTP " + code);
                    }
                }
            } catch (IOException e) {
                if (debug) logger.warning("[HTTP] ReportStats error: " + e.getMessage());
            }
        }, httpExecutor);
    }

    private void sendInterserverEventPoll() {
        if (!isConnected() || sessionId == null || isServerInErrorState()) {
            return;
        }
        if (!interserverPollInFlight.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String url = serverAddress + "/api/v1/events/poll";
                JsonObject json = createBasePayload();
                addSession(json);

                Request request = new Request.Builder()
                        .url(url)
                        .post(jsonBody(json))
                        .header("X-API-Key", apiKey)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    int code = response.code();
                    ResponseBody respBody = response.body();
                    String responseBody = respBody != null ? respBody.string() : "";
                    handleApiWarnings(responseBody);
                    if (code == 404 || code == 405) {
                        return;
                    }
                    if (code == 401 || code == 403) {
                        connected.set(false);
                        return;
                    }
                    if (response.isSuccessful()) {
                        handleInterserverEvents(responseBody);
                    } else if (debug) {
                        logger.warning("[HTTP] Inter-server event poll failed: HTTP " + code);
                    }
                }
            } catch (IOException e) {
                if (debug) logger.warning("[HTTP] Inter-server event poll error: " + e.getMessage());
            } finally {
                interserverPollInFlight.set(false);
            }
        }, httpExecutor);
    }

    private void sendLegacyReportStats(JsonObject json) throws IOException {
        String url = serverAddress + "/api/v1/reportstats";
        Request request = new Request.Builder()
                .url(url)
                .post(jsonBody(json))
                .header("X-API-Key", apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            int code = response.code();
            ResponseBody respBody = response.body();
            String responseBody = respBody != null ? respBody.string() : "";
            handleApiWarnings(responseBody);
            if (response.isSuccessful()) {
                limitExceeded = false;
                serverErrorState = false;
                exitStasisMode();
            } else if (code == 429) {
                handleRateLimitError();
            } else if (code >= 500) {
                logger.warning("[HTTP] ReportStats received server error " + code);
                enterServerErrorState("ReportStats received HTTP " + code);
            }
        }
    }

    private void scheduleReconnect() {
        if (shuttingDown.get() || !autoReconnectEnabled) return;
        if (reconnectTask.get() != null) {
            logger.info("[HTTP] Reconnect already scheduled, skipping");
            return;
        }
        logger.info("[HTTP] Scheduling reconnect in 10 seconds...");
        ScheduledTask task = SchedulerManager.getAdapter().runAsyncDelayed(() -> {
            reconnectTask.set(null);
            if (!shuttingDown.get() && autoReconnectEnabled && !connected.get()) {
                connect().thenAccept(success -> {
                    if (!success) {
                        scheduleReconnect();
                    } else {
                        logger.info("[HTTP] Reconnected successfully");
                    }
                });
            }
        }, 200);
        reconnectTask.set(task);
    }

    @Override
    public io.reactivex.rxjava3.core.Observable<AIResponse> predict(byte[] playerData, String playerUuid, String playerName) {
        if (!isConnected()) {
            return io.reactivex.rxjava3.core.Observable.error(
                    new IllegalStateException("Not connected to HTTP server"));
        }
        if (inStasisMode.get()) {
            return io.reactivex.rxjava3.core.Observable.error(
                    new IllegalStateException("API in stasis mode (rate limited)"));
        }
        if (limitExceeded) {
            return io.reactivex.rxjava3.core.Observable.error(
                    new IllegalStateException("Request limit | Upgrade tariff"));
        }
        if (isServerInErrorState()) {
            return io.reactivex.rxjava3.core.Observable.error(
                    new IllegalStateException("Server error state active, Predict blocked"));
        }

        return io.reactivex.rxjava3.core.Observable.create(emitter -> {
            CompletableFuture.runAsync(() -> {
                try {
                    JsonObject payload = createPredictPayload(playerData, playerUuid, playerName);
                    boolean streamed = executeStreamingPredict(payload, emitter);
                    if (!streamed && !emitter.isDisposed()) {
                        AIResponse response = executeLegacyPredict(payload);
                        emitter.onNext(response);
                    }
                    if (!emitter.isDisposed()) {
                        emitter.onComplete();
                    }
                } catch (Exception e) {
                    String msg = e.getMessage();
                    if (msg != null && (msg.contains("Server error") || msg.contains("503") || msg.contains("500"))) {
                        enterServerErrorState(msg);
                    }
                    if (!emitter.isDisposed()) {
                        emitter.onError(new RuntimeException(e.getMessage()));
                    }
                }
            }, httpExecutor);
        });
    }

    private JsonObject createPredictPayload(byte[] playerData, String playerUuid, String playerName) {
        String dataBase64 = java.util.Base64.getEncoder().encodeToString(playerData);
        JsonObject json = createBasePayload();
        addSession(json);
        json.addProperty("playerData", dataBase64);
        json.addProperty("playerUuid", playerUuid);
        json.addProperty("playerName", playerName);
        return json;
    }

    private AIResponse executeLegacyPredict(JsonObject json) throws IOException {
        String url = serverAddress + "/api/v1/predict";
        Request request = new Request.Builder()
                .url(url)
                .post(jsonBody(json))
                .header("X-API-Key", apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody respBody = response.body();
            String responseBody = respBody != null ? respBody.string() : "";
            int code = response.code();
            handleApiWarnings(responseBody);
            handlePredictStatus(code, responseBody);
            return parsePredictResponse(responseBody);
        }
    }

    private boolean executeStreamingPredict(JsonObject json,
            io.reactivex.rxjava3.core.ObservableEmitter<AIResponse> emitter) throws IOException {
        String url = serverAddress + "/api/v1/predict-stream";
        Request request = new Request.Builder()
                .url(url)
                .post(jsonBody(json))
                .header("X-API-Key", apiKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            int code = response.code();
            if (code == 404 || code == 405) {
                return false;
            }
            ResponseBody respBody = response.body();
            if (respBody == null) {
                handlePredictStatus(code, "");
                return false;
            }
            if (!response.isSuccessful()) {
                String responseBody = respBody.string();
                handleApiWarnings(responseBody);
                handlePredictStatus(code, responseBody);
                return false;
            }

            boolean emittedAny = false;
            try (BufferedReader reader = new BufferedReader(respBody.charStream())) {
                String line;
                while (!emitter.isDisposed() && (line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    handleApiWarnings(line);
                    JsonObject object = new JsonParser().parse(line).getAsJsonObject();
                    String type = object.has("type") ? object.get("type").getAsString() : "prediction";
                    if ("done".equalsIgnoreCase(type)) {
                        break;
                    }
                    if ("error".equalsIgnoreCase(type)) {
                        if (debug) logger.warning("[HTTP] Streaming model error: " + line);
                        continue;
                    }
                    AIResponse aiResponse = parsePredictResponse(line);
                    emitter.onNext(aiResponse);
                    emittedAny = true;
                }
            }
            return emittedAny;
        }
    }

    private void handlePredictStatus(int code, String responseBody) {
        if (code == 401 || code == 403) {
            logger.severe("[HTTP] Authentication failed! API key is invalid, expired, or corrupted. Please check your API key in config.yml");
            connected.set(false);
            throw new RuntimeException("API key is invalid or corrupted");
        }
        if (code == 429) {
            handleRateLimitError();
            throw new RuntimeException("Request limit");
        }
        if (code >= 500) {
            enterServerErrorState("Server error HTTP " + code + ": " + responseBody);
            throw new RuntimeException("Server error HTTP " + code + " - entering silent mode");
        }
        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code + ": " + responseBody);
        }
    }

    @Override
    public CompletableFuture<Boolean> reportAlert(String playerUuid, String playerName,
            String model, double probability, double buffer) {
        if (!eventReportingEnabled || probability < apiAlertEventThreshold) {
            return CompletableFuture.completedFuture(false);
        }
        JsonObject json = createEventPayload("alert", playerUuid, playerName, model,
                probability, buffer, 0, "alert", "");
        return sendEvent(json);
    }

    @Override
    public CompletableFuture<Boolean> reportPunish(String playerUuid, String playerName,
            String model, double probability, double buffer, int violationLevel,
            String action, String command) {
        if (!eventReportingEnabled) {
            return CompletableFuture.completedFuture(false);
        }
        JsonObject json = createEventPayload("punish", playerUuid, playerName, model,
                probability, buffer, violationLevel, action, command);
        return sendEvent(json);
    }

    private JsonObject createEventPayload(String type, String playerUuid, String playerName,
            String model, double probability, double buffer, int violationLevel,
            String action, String command) {
        JsonObject json = createBasePayload();
        addSession(json);
        json.addProperty("type", type);
        json.addProperty("playerUuid", playerUuid);
        json.addProperty("playerId", playerUuid);
        json.addProperty("playerName", playerName);
        json.addProperty("model", model);
        json.addProperty("probability", probability);
        json.addProperty("buffer", buffer);
        json.addProperty("vl", violationLevel);
        json.addProperty("violationLevel", violationLevel);
        json.addProperty("action", action != null ? action : type);
        json.addProperty("command", command != null ? command : "");
        return json;
    }

    private CompletableFuture<Boolean> sendEvent(JsonObject json) {
        if (!isConnected() || isServerInErrorState()) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = serverAddress + "/api/v1/events";
                Request request = new Request.Builder()
                        .url(url)
                        .post(jsonBody(json))
                        .header("X-API-Key", apiKey)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    ResponseBody respBody = response.body();
                    String responseBody = respBody != null ? respBody.string() : "";
                    handleApiWarnings(responseBody);
                    int code = response.code();
                    if (code == 404 || code == 405) {
                        return false;
                    }
                    if (code == 401 || code == 403) {
                        connected.set(false);
                        return false;
                    }
                    if (code >= 500) {
                        enterServerErrorState("Event report received HTTP " + code);
                        return false;
                    }
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                if (debug) {
                    logger.warning("[HTTP] Event report failed: " + e.getMessage());
                }
                return false;
            }
        }, httpExecutor);
    }

    private AIResponse parsePredictResponse(String responseBody) {
        try {
            com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(responseBody).getAsJsonObject();
            double probability = json.has("probability") ? json.get("probability").getAsDouble() : 0.0;
            String model = json.has("model") ? json.get("model").getAsString() : null;
            String error = json.has("error") ? json.get("error").getAsString() : null;

            if (error != null && !error.isEmpty()) {
                throw new RuntimeException(error);
            }

            return new AIResponse(probability, null, model);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public boolean isLimitExceeded() {
        return limitExceeded;
    }

    @Override
    public boolean isServerErrorState() {
        return serverErrorState;
    }

    private boolean isServerInErrorState() {
        if (!serverErrorState) return false;
        if (System.currentTimeMillis() - lastServerErrorTime > SERVER_ERROR_SILENCE_MS) {
            logger.info("[HTTP] Server error state expired, clearing");
            serverErrorState = false;
            return false;
        }
        return true;
    }

    private void enterServerErrorState(String reason) {
        serverErrorState = true;
        lastServerErrorTime = System.currentTimeMillis();
        logger.warning("[HTTP] Entering server error state: " + reason);
        scheduleReconnect();
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String getServerAddress() {
        return serverAddress;
    }

    public void setAutoReconnectEnabled(boolean enabled) {
        this.autoReconnectEnabled = enabled;
    }

    public boolean isAutoReconnectEnabled() {
        return autoReconnectEnabled;
    }

    public boolean isInStasisMode() {
        return inStasisMode.get();
    }

    private void handleRateLimitError() {
        limitExceeded = true;
        logger.warning("[HTTP] Request limit | Upgrade tariff - entering stasis mode");
        enterStasisMode();
    }

    private void enterStasisMode() {
        if (inStasisMode.compareAndSet(false, true)) {
            logger.warning("[HTTP] Entering stasis mode - stopping all requests");
            
            // Stop all periodic tasks
            ScheduledTask hb = heartbeatTask.getAndSet(null);
            if (hb != null) hb.cancel();
            ScheduledTask rs = reportStatsTask.getAndSet(null);
            if (rs != null) rs.cancel();
            ScheduledTask ie = interserverEventTask.getAndSet(null);
            if (ie != null) ie.cancel();
            ScheduledTask pc = periodicCheckTask.getAndSet(null);
            if (pc != null) pc.cancel();
            
            // Start stasis check task (every 5 minutes)
            startStasisCheck();
        }
    }

    private void exitStasisMode() {
        if (inStasisMode.compareAndSet(true, false)) {
            logger.info("[HTTP] Exiting stasis mode - resuming normal operation");
            
            // Stop stasis check
            ScheduledTask st = stasisCheckTask.getAndSet(null);
            if (st != null) st.cancel();
            
            // Restart normal tasks
            startHeartbeat();
            startReportStats();
            startInterserverEventPoll();
            startPeriodicCheck();
        }
    }

    private void startStasisCheck() {
        ScheduledTask existing = stasisCheckTask.getAndSet(null);
        if (existing != null) existing.cancel();

        stasisCheckTask.set(SchedulerManager.getAdapter().runAsyncRepeating(() -> {
            if (shuttingDown.get()) return;
            checkApiAvailability();
        }, 100, STASIS_CHECK_INTERVAL_MS / 50));
    }

    private void checkApiAvailability() {
        if (debug) logger.info("[HTTP] Stasis check: testing API availability...");
        
        CompletableFuture.runAsync(() -> {
            try {
                String url = serverAddress + "/api/v1/heartbeat";
                JsonObject json = createBasePayload();
                addSession(json);
                addOnline(json);

                RequestBody body = jsonBody(json);
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header("X-API-Key", apiKey)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    int code = response.code();
                    if (response.isSuccessful()) {
                        logger.info("[HTTP] API is available again - exiting stasis mode");
                        limitExceeded = false;
                        exitStasisMode();
                    } else if (code == 429) {
                        if (debug) logger.info("[HTTP] Still rate limited, remaining in stasis mode");
                    } else {
                        if (debug) logger.warning("[HTTP] Stasis check failed with code: " + code);
                    }
                }
            } catch (Exception e) {
                if (debug) logger.warning("[HTTP] Stasis check error: " + e.getMessage());
            }
        }, httpExecutor);
    }

    public CompletableFuture<Long> measureLatency() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = serverAddress + "/api/v1/heartbeat";
                JsonObject json = createBasePayload();
                addSession(json);
                addOnline(json);

                RequestBody body = jsonBody(json);
                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header("X-API-Key", apiKey)
                        .build();

                long start = System.currentTimeMillis();
                try (Response response = httpClient.newCall(request).execute()) {
                    long end = System.currentTimeMillis();
                    if (response.isSuccessful()) {
                        return end - start;
                    }
                    return -1L;
                }
            } catch (Exception e) {
                return -1L;
            }
        }, httpExecutor);
    }
}
