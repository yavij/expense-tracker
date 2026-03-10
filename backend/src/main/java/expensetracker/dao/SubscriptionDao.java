package expensetracker.dao;

import expensetracker.util.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SubscriptionDao {

    public Map<String, Object> findActiveByUserId(String userId) {
        String sql = "SELECT * FROM subscriptions WHERE user_id = ? AND status = 'ACTIVE' AND expires_at > ? ORDER BY expires_at DESC LIMIT 1";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rowToMap(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find active subscription", e);
        }
    }

    public Map<String, Object> insert(String id, String userId, String orderId, String paymentId, double amount, Timestamp expiresAt) {
        String sql = "INSERT INTO subscriptions (id, user_id, razorpay_order_id, razorpay_payment_id, amount, status, activated_at, expires_at) VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, id);
            ps.setString(2, userId);
            ps.setString(3, orderId);
            ps.setString(4, paymentId);
            ps.setDouble(5, amount);
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, expiresAt);
            ps.executeUpdate();

            Map<String, Object> sub = new LinkedHashMap<>();
            sub.put("id", id);
            sub.put("userId", userId);
            sub.put("status", "ACTIVE");
            sub.put("activatedAt", now.toString());
            sub.put("expiresAt", expiresAt.toString());
            return sub;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert subscription", e);
        }
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getString("id"));
        m.put("userId", rs.getString("user_id"));
        m.put("razorpayOrderId", rs.getString("razorpay_order_id"));
        m.put("razorpayPaymentId", rs.getString("razorpay_payment_id"));
        m.put("amount", rs.getDouble("amount"));
        m.put("status", rs.getString("status"));
        m.put("activatedAt", rs.getTimestamp("activated_at").toString());
        m.put("expiresAt", rs.getTimestamp("expires_at").toString());
        return m;
    }
}
