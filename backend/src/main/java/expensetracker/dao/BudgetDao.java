package expensetracker.dao;

import expensetracker.model.Budget;
import expensetracker.util.Database;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BudgetDao {

    private static String mCol() { return Database.isH2() ? "\"month\"" : "month"; }

    public List<Budget> findByUser(String userId) {
        String sql = "SELECT id, user_id, category, monthly_limit, alert_threshold, " + mCol() + ", created_at FROM budgets WHERE user_id = ? ORDER BY " + mCol() + " DESC, category ASC";
        List<Budget> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<Budget> findByUserAndMonth(String userId, String month) {
        String sql = "SELECT id, user_id, category, monthly_limit, alert_threshold, " + mCol() + ", created_at FROM budgets WHERE user_id = ? AND (" + mCol() + " = ? OR " + mCol() + " IS NULL) ORDER BY category ASC";
        List<Budget> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Optional<Budget> findByIdAndUser(String id, String userId) {
        String sql = "SELECT id, user_id, category, monthly_limit, alert_threshold, " + mCol() + ", created_at FROM budgets WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public Budget upsert(Budget budget) {
        // Check if exists
        Optional<Budget> existing = findByIdAndUser(budget.getId(), budget.getUserId());
        if (existing.isPresent()) {
            return update(budget);
        } else {
            return insert(budget);
        }
    }

    private Budget insert(Budget budget) {
        String id = budget.getId() != null ? budget.getId() : UUID.randomUUID().toString();
        String sql = "INSERT INTO budgets (id, user_id, category, monthly_limit, alert_threshold, " + mCol() + ") VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, budget.getUserId());
            ps.setString(3, budget.getCategory());
            ps.setDouble(4, budget.getMonthlyLimit());
            ps.setInt(5, budget.getAlertThreshold());
            ps.setString(6, budget.getMonth());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return findByIdAndUser(id, budget.getUserId()).orElseThrow();
    }

    private Budget update(Budget budget) {
        String sql = "UPDATE budgets SET category = ?, monthly_limit = ?, alert_threshold = ?, " + mCol() + " = ? WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, budget.getCategory());
            ps.setDouble(2, budget.getMonthlyLimit());
            ps.setInt(3, budget.getAlertThreshold());
            ps.setString(4, budget.getMonth());
            ps.setString(5, budget.getId());
            ps.setString(6, budget.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return findByIdAndUser(budget.getId(), budget.getUserId()).orElseThrow();
    }

    public boolean delete(String id, String userId) {
        String sql = "DELETE FROM budgets WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Budget mapRow(ResultSet rs) throws SQLException {
        Budget b = new Budget();
        b.setId(rs.getString("id"));
        b.setUserId(rs.getString("user_id"));
        b.setCategory(rs.getString("category"));
        b.setMonthlyLimit(rs.getDouble("monthly_limit"));
        b.setAlertThreshold(rs.getInt("alert_threshold"));
        b.setMonth(rs.getString("month"));
        Timestamp ts = rs.getTimestamp("created_at");
        b.setCreatedAt(ts != null ? ts.toInstant() : null);
        return b;
    }
}
