package expensetracker.dao;

import expensetracker.model.LoginEvent;
import expensetracker.util.Database;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LoginEventDao {

    public void insert(String userId, String ipAddress) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO login_events (id, user_id, ip_address) VALUES (?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            ps.setString(3, ipAddress);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<LoginEvent> findAllWithUserEmail(int limit, int offset) {
        String sql = "SELECT e.id, e.user_id, e.logged_in_at, e.ip_address, u.email AS user_email " +
                "FROM login_events e JOIN users u ON e.user_id = u.id ORDER BY e.logged_in_at DESC LIMIT ? OFFSET ?";
        List<LoginEvent> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LoginEvent e = new LoginEvent();
                    e.setId(rs.getString("id"));
                    e.setUserId(rs.getString("user_id"));
                    Timestamp ts = rs.getTimestamp("logged_in_at");
                    e.setLoggedInAt(ts != null ? ts.toInstant() : null);
                    e.setIpAddress(rs.getString("ip_address"));
                    e.setUserEmail(rs.getString("user_email"));
                    list.add(e);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return list;
    }
}
