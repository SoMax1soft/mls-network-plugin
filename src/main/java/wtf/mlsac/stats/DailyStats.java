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

package wtf.mlsac.stats;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class DailyStats {
    private final JavaPlugin plugin;
    private final Logger logger;
    private Connection connection;
    private final AtomicInteger todayDetections = new AtomicInteger(0);
    private final AtomicInteger todayRequests = new AtomicInteger(0);
    private String currentDate;

    public DailyStats(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.currentDate = LocalDate.now().toString();
    }

    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            String dbPath = new File(dataFolder, "stats.db").getAbsolutePath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            createTables();
            loadTodayStats();
            
            logger.info("[Stats] Database initialized");
        } catch (SQLException e) {
            logger.severe("[Stats] Failed to initialize database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS daily_stats (" +
                     "date TEXT PRIMARY KEY," +
                     "detections INTEGER DEFAULT 0," +
                     "requests INTEGER DEFAULT 0" +
                     ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void loadTodayStats() {
        String today = LocalDate.now().toString();
        if (!today.equals(currentDate)) {
            currentDate = today;
            todayDetections.set(0);
            todayRequests.set(0);
        }

        String sql = "SELECT detections, requests FROM daily_stats WHERE date = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, currentDate);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                todayDetections.set(rs.getInt("detections"));
                todayRequests.set(rs.getInt("requests"));
            }
        } catch (SQLException e) {
            logger.warning("[Stats] Failed to load today's stats: " + e.getMessage());
        }
    }

    public void incrementDetections() {
        checkDateRollover();
        todayDetections.incrementAndGet();
        saveStats();
    }

    public void incrementRequests() {
        checkDateRollover();
        todayRequests.incrementAndGet();
        saveStats();
    }

    private void checkDateRollover() {
        String today = LocalDate.now().toString();
        if (!today.equals(currentDate)) {
            currentDate = today;
            todayDetections.set(0);
            todayRequests.set(0);
        }
    }

    private void saveStats() {
        String sql = "INSERT OR REPLACE INTO daily_stats (date, detections, requests) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, currentDate);
            stmt.setInt(2, todayDetections.get());
            stmt.setInt(3, todayRequests.get());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning("[Stats] Failed to save stats: " + e.getMessage());
        }
    }

    public int getTodayDetections() {
        checkDateRollover();
        return todayDetections.get();
    }

    public int getTodayRequests() {
        checkDateRollover();
        return todayRequests.get();
    }

    public void shutdown() {
        if (connection != null) {
            try {
                saveStats();
                connection.close();
                logger.info("[Stats] Database closed");
            } catch (SQLException e) {
                logger.warning("[Stats] Error closing database: " + e.getMessage());
            }
        }
    }
}
