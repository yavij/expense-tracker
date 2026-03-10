package expensetracker.dao;

import expensetracker.model.DebtEntry;
import expensetracker.util.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DebtDao {

    public List<DebtEntry> findByUserAndStatus(String userId, String status) {
        StringBuilder sql = new StringBuilder("SELECT id, user_id, name, type, principal_amount, interest_rate, emi_amount, remaining_balance, start_date, end_date, priority, status, created_at FROM debt_entries WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_at DESC");

        List<DebtEntry> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                ps.setString(i + 1, (String) p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Optional<DebtEntry> findByIdAndUser(String id, String userId) {
        String sql = "SELECT id, user_id, name, type, principal_amount, interest_rate, emi_amount, remaining_balance, start_date, end_date, priority, status, created_at FROM debt_entries WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public DebtEntry insert(DebtEntry d) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO debt_entries (id, user_id, name, type, principal_amount, interest_rate, emi_amount, remaining_balance, start_date, end_date, priority, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, d.getUserId());
            ps.setString(3, d.getName());
            ps.setString(4, d.getType());
            ps.setBigDecimal(5, d.getPrincipalAmount());
            ps.setBigDecimal(6, d.getInterestRate());
            ps.setBigDecimal(7, d.getEmiAmount());
            ps.setBigDecimal(8, d.getRemainingBalance());
            ps.setObject(9, d.getStartDate());
            ps.setObject(10, d.getEndDate());
            ps.setInt(11, d.getPriority());
            ps.setString(12, d.getStatus());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, d.getUserId()).orElseThrow();
    }

    public Optional<DebtEntry> update(String id, String userId, String name, String type, BigDecimal principalAmount, BigDecimal interestRate, BigDecimal emiAmount, BigDecimal remainingBalance, LocalDate startDate, LocalDate endDate, int priority, String status) {
        String sql = "UPDATE debt_entries SET name = ?, type = ?, principal_amount = ?, interest_rate = ?, emi_amount = ?, remaining_balance = ?, start_date = ?, end_date = ?, priority = ?, status = ? WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setBigDecimal(3, principalAmount);
            ps.setBigDecimal(4, interestRate);
            ps.setBigDecimal(5, emiAmount);
            ps.setBigDecimal(6, remainingBalance);
            ps.setObject(7, startDate);
            ps.setObject(8, endDate);
            ps.setInt(9, priority);
            ps.setString(10, status);
            ps.setString(11, id);
            ps.setString(12, userId);
            if (ps.executeUpdate() == 0) return Optional.empty();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, userId);
    }

    public void updateBalance(String id, String userId, BigDecimal newBalance) {
        String sql = "UPDATE debt_entries SET remaining_balance = ? WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, id);
            ps.setString(3, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean delete(String id, String userId) {
        String sql = "DELETE FROM debt_entries WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal sumRemainingByUser(String userId) {
        String sql = "SELECT COALESCE(SUM(remaining_balance), 0) FROM debt_entries WHERE user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return BigDecimal.ZERO;
    }

    public List<DebtEntry> findByUserOrderByPriority(String userId) {
        String sql = "SELECT id, user_id, name, type, principal_amount, interest_rate, emi_amount, remaining_balance, start_date, end_date, priority, status, created_at FROM debt_entries WHERE user_id = ? AND status = ? ORDER BY priority ASC";
        List<DebtEntry> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, DebtEntry.ACTIVE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private static DebtEntry mapRow(ResultSet rs) throws SQLException {
        DebtEntry d = new DebtEntry();
        d.setId(rs.getString("id"));
        d.setUserId(rs.getString("user_id"));
        d.setName(rs.getString("name"));
        d.setType(rs.getString("type"));
        d.setPrincipalAmount(rs.getBigDecimal("principal_amount"));
        d.setInterestRate(rs.getBigDecimal("interest_rate"));
        d.setEmiAmount(rs.getBigDecimal("emi_amount"));
        d.setRemainingBalance(rs.getBigDecimal("remaining_balance"));
        Date sd = rs.getDate("start_date");
        d.setStartDate(sd != null ? sd.toLocalDate() : null);
        Date ed = rs.getDate("end_date");
        d.setEndDate(ed != null ? ed.toLocalDate() : null);
        d.setPriority(rs.getInt("priority"));
        d.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        d.setCreatedAt(ts != null ? ts.toInstant() : null);
        return d;
    }
}
