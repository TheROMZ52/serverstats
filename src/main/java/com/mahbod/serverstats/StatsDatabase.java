package com.mahbod.serverstats;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles all SQLite reads/writes. One connection, synchronized access
 * (fine for a small Minecraft server's write volume).
 */
public class StatsDatabase {

    private final Logger logger;
    private Connection connection;

    public StatsDatabase(File dataFolder, Logger logger) {
        this.logger = logger;
        try {
            if (!dataFolder.exists()) dataFolder.mkdirs();
            Class.forName("com.mahbod.serverstats.libs.sqlite.JDBC");
            String url = "jdbc:sqlite:" + new File(dataFolder, "stats.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);
            createTables();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    name TEXT,
                    first_join INTEGER,
                    last_seen INTEGER,
                    playtime_seconds INTEGER DEFAULT 0,
                    kills INTEGER DEFAULT 0,
                    deaths INTEGER DEFAULT 0,
                    blocks_broken INTEGER DEFAULT 0,
                    blocks_placed INTEGER DEFAULT 0
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT,
                    player TEXT,
                    detail TEXT,
                    timestamp INTEGER
                )
            """);
        }
    }

    public synchronized void upsertPlayerJoin(String uuid, String name, long now) {
        String sql = """
            INSERT INTO players (uuid, name, first_join, last_seen)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, last_seen = excluded.last_seen
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "upsertPlayerJoin failed", e);
        }
        logEvent("join", name, "", now);
    }

    public synchronized void updateLastSeenAndPlaytime(String uuid, long now, long sessionSeconds) {
        String sql = "UPDATE players SET last_seen = ?, playtime_seconds = playtime_seconds + ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, now);
            ps.setLong(2, sessionSeconds);
            ps.setString(3, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "updateLastSeenAndPlaytime failed", e);
        }
    }

    public synchronized void incrementKill(String uuid, long now) {
        incrementColumn(uuid, "kills");
    }

    public synchronized void incrementDeath(String uuid, long now) {
        incrementColumn(uuid, "deaths");
    }

    public synchronized void incrementBlocksBroken(String uuid) {
        incrementColumn(uuid, "blocks_broken");
    }

    public synchronized void incrementBlocksPlaced(String uuid) {
        incrementColumn(uuid, "blocks_placed");
    }

    private void incrementColumn(String uuid, String column) {
        String sql = "UPDATE players SET " + column + " = " + column + " + 1 WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "incrementColumn(" + column + ") failed", e);
        }
    }

    public synchronized void logEvent(String type, String player, String detail, long now) {
        String sql = "INSERT INTO events (type, player, detail, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, player);
            ps.setString(3, detail);
            ps.setLong(4, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "logEvent failed", e);
        }
    }

    /** Returns all players as a list of maps, ordered by playtime desc. */
    public synchronized List<Map<String, Object>> getAllPlayers() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT * FROM players ORDER BY playtime_seconds DESC";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("uuid", rs.getString("uuid"));
                row.put("name", rs.getString("name"));
                row.put("first_join", rs.getLong("first_join"));
                row.put("last_seen", rs.getLong("last_seen"));
                row.put("playtime_seconds", rs.getLong("playtime_seconds"));
                row.put("kills", rs.getInt("kills"));
                row.put("deaths", rs.getInt("deaths"));
                row.put("blocks_broken", rs.getInt("blocks_broken"));
                row.put("blocks_placed", rs.getInt("blocks_placed"));
                result.add(row);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "getAllPlayers failed", e);
        }
        return result;
    }

    /** Returns the most recent N events, newest first. */
    public synchronized List<Map<String, Object>> getRecentEvents(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT * FROM events ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("type", rs.getString("type"));
                    row.put("player", rs.getString("player"));
                    row.put("detail", rs.getString("detail"));
                    row.put("timestamp", rs.getLong("timestamp"));
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "getRecentEvents failed", e);
        }
        return result;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to close database", e);
        }
    }
}
