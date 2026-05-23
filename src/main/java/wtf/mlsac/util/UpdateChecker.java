package wtf.mlsac.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;
import wtf.mlsac.config.Config;
import wtf.mlsac.scheduler.ScheduledTask;
import wtf.mlsac.scheduler.SchedulerManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class UpdateChecker {
    private static final long CHECK_INTERVAL_TICKS = 5L * 60L * 20L;
    private static final int CONNECT_TIMEOUT_MILLIS = 8000;
    private static final int READ_TIMEOUT_MILLIS = 45000;

    private final JavaPlugin plugin;
    private final Config config;
    private final String currentVersion;
    private final String apiBaseUrl;
    private final int javaBuild;
    private final AtomicBoolean checking = new AtomicBoolean(false);

    private ScheduledTask task;
    private volatile String latestVersion;
    private volatile boolean updateAvailable = false;
    private volatile boolean updateDownloaded = false;

    public UpdateChecker(JavaPlugin plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.currentVersion = plugin.getDescription().getVersion();
        this.apiBaseUrl = normalizeApiBaseUrl(config.getServerAddress());
        this.javaBuild = Runtime.version().feature() >= 21 ? 21 : 17;
    }

    public void start() {
        stop();
        if (!config.isUpdatesEnabled()) {
            plugin.getLogger().info("[Updater] Automatic updates are disabled in config.yml");
            return;
        }

        checkForUpdates();
        task = SchedulerManager.getAdapter().runAsyncRepeating(this::checkForUpdates, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public CompletableFuture<Boolean> checkForUpdates() {
        if (!config.isUpdatesEnabled() || currentVersion.toLowerCase(Locale.ROOT).contains("dev")) {
            return CompletableFuture.completedFuture(false);
        }

        if (!checking.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(updateAvailable);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                UpdateInfo info = requestUpdateInfo();
                if (info == null) {
                    updateAvailable = false;
                    return false;
                }

                latestVersion = info.version;
                updateAvailable = info.updateAvailable;

                if (!info.updateAvailable) {
                    updateDownloaded = false;
                    return false;
                }

                if (isExpectedUpdateAlreadyDownloaded(info)) {
                    updateDownloaded = true;
                    return true;
                }

                downloadUpdate(info);
                updateDownloaded = true;
                plugin.getLogger().warning("[Updater] MLSAC " + info.version + " for Java " + info.javaVersion
                        + " was downloaded to plugins/update. Restart the server to apply it.");
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[Updater] Failed to check or download update: " + e.getMessage());
                return false;
            } finally {
                checking.set(false);
            }
        });
    }

    private UpdateInfo requestUpdateInfo() throws Exception {
        String url = apiBaseUrl + "/plugin/update?java=" + javaBuild
                + "&version=" + URLEncoder.encode(currentVersion, StandardCharsets.UTF_8.name());
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        if (connection.getResponseCode() != 200) {
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("success") || !root.get("success").getAsBoolean()) {
                return null;
            }

            JsonObject data = root.getAsJsonObject("data");
            if (data == null) {
                return null;
            }

            UpdateInfo info = new UpdateInfo();
            info.version = getString(data, "version");
            info.fileName = getString(data, "fileName");
            info.sha256 = getString(data, "sha256");
            info.downloadPath = getString(data, "downloadPath");
            info.javaVersion = data.has("javaVersion") ? data.get("javaVersion").getAsInt() : javaBuild;
            info.updateAvailable = data.has("updateAvailable") && data.get("updateAvailable").getAsBoolean();
            return info;
        }
    }

    private void downloadUpdate(UpdateInfo info) throws Exception {
        File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
        if (!updateFolder.exists() && !updateFolder.mkdirs()) {
            throw new IllegalStateException("Failed to create update folder: " + updateFolder.getAbsolutePath());
        }

        File currentJar = getCurrentPluginJar();
        String targetName = currentJar != null ? currentJar.getName() : "MLSAC.jar";
        File targetFile = new File(updateFolder, targetName);
        File tmpFile = new File(updateFolder, targetName + ".tmp");

        String downloadUrl = buildDownloadUrl(info.downloadPath);
        HttpURLConnection connection = openConnection(downloadUrl);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/java-archive");

        if (connection.getResponseCode() != 200) {
            throw new IllegalStateException("Update download returned HTTP " + connection.getResponseCode());
        }

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(tmpFile)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }

        String actualSha256 = sha256(tmpFile);
        if (info.sha256 != null && !info.sha256.isEmpty() && !actualSha256.equalsIgnoreCase(info.sha256)) {
            Files.deleteIfExists(tmpFile.toPath());
            throw new IllegalStateException("Downloaded update SHA256 mismatch");
        }

        Files.move(tmpFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private boolean isExpectedUpdateAlreadyDownloaded(UpdateInfo info) {
        try {
            File currentJar = getCurrentPluginJar();
            String targetName = currentJar != null ? currentJar.getName() : "MLSAC.jar";
            File updateFile = new File(new File(plugin.getDataFolder().getParentFile(), "update"), targetName);
            return updateFile.isFile()
                    && info.sha256 != null
                    && !info.sha256.isEmpty()
                    && sha256(updateFile).equalsIgnoreCase(info.sha256);
        } catch (Exception ignored) {
            return false;
        }
    }

    private HttpURLConnection openConnection(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", "MLSAC-Plugin-Updater/" + currentVersion);
        return connection;
    }

    private String buildDownloadUrl(String downloadPath) {
        if (downloadPath == null || downloadPath.isEmpty()) {
            return apiBaseUrl + "/plugin/update/download/java" + javaBuild;
        }

        if (downloadPath.startsWith("http://") || downloadPath.startsWith("https://")) {
            return downloadPath;
        }

        if (downloadPath.startsWith("/api/v1/")) {
            String marker = "/api/v1";
            int index = apiBaseUrl.indexOf(marker);
            if (index >= 0) {
                return apiBaseUrl.substring(0, index) + downloadPath;
            }
        }

        if (downloadPath.startsWith("/")) {
            String root = apiBaseUrl;
            int schemeIndex = root.indexOf("://");
            if (schemeIndex >= 0) {
                int slashIndex = root.indexOf('/', schemeIndex + 3);
                if (slashIndex >= 0) {
                    root = root.substring(0, slashIndex);
                }
            }
            return root + downloadPath;
        }

        return apiBaseUrl + "/" + downloadPath;
    }

    private File getCurrentPluginJar() {
        try {
            File file = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            return file.isFile() ? file : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        StringBuilder builder = new StringBuilder();
        for (byte b : digest.digest()) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static String normalizeApiBaseUrl(String value) {
        String result = value == null || value.trim().isEmpty()
                ? Config.DEFAULT_SERVER_ADDRESS
                : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        if (!result.endsWith("/api/v1")) {
            result = result + "/api/v1";
        }
        return result;
    }

    private static String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public boolean isUpdateDownloaded() {
        return updateDownloaded;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    private static final class UpdateInfo {
        private String version;
        private String fileName;
        private String sha256;
        private String downloadPath;
        private int javaVersion;
        private boolean updateAvailable;
    }
}
