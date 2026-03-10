package expensetracker.dao;

import expensetracker.util.Database;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserPreferenceDao {

    /** Get all preferences for a user as a key→value map. */
    public Map<String, String> findByUserId(String userId) throws SQLException {
        String sql = "SELECT pref_key, pref_value FROM user_preferences WHERE user_id = ?";
        Map<String, String> prefs = new HashMap<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    prefs.put(rs.getString("pref_key"), rs.getString("pref_value"));
                }
            }
        }
        return prefs;
    }

    /** Upsert a single preference. */
    public void upsert(String userId, String key, String value) throws SQLException {
        // Use MERGE for H2, INSERT ... ON DUPLICATE KEY UPDATE for MySQL
        String sql = Database.isH2()
                ? "MERGE INTO user_preferences (user_id, pref_key, pref_value) KEY (user_id, pref_key) VALUES (?, ?, ?)"
                : "INSERT INTO user_preferences (user_id, pref_key, pref_value) VALUES (?, ?, ?) "
                  + "ON DUPLICATE KEY UPDATE pref_value = VALUES(pref_value)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, key);
            ps.setString(3, value);
            ps.executeUpdate();
        }
    }

    /** Upsert multiple preferences in a single transaction. */
    public void upsertAll(String userId, Map<String, String> prefs) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            String sql = Database.isH2()
                    ? "MERGE INTO user_preferences (user_id, pref_key, pref_value) KEY (user_id, pref_key) VALUES (?, ?, ?)"
                    : "INSERT INTO user_preferences (user_id, pref_key, pref_value) VALUES (?, ?, ?) "
                      + "ON DUPLICATE KEY UPDATE pref_value = VALUES(pref_value)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (Map.Entry<String, String> entry : prefs.entrySet()) {
                    ps.setString(1, userId);
                    ps.setString(2, entry.getKey());
                    ps.setString(3, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Delete a single preference. */
    public void delete(String userId, String key) throws SQLException {
        String sql = "DELETE FROM user_preferences WHERE user_id = ? AND pref_key = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, key);
            ps.executeUpdate();
        }
    }
}
