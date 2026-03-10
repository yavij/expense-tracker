package expensetracker.dao;

import expensetracker.model.DebtPayment;
import expensetracker.util.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DebtPaymentDao {

    public List<DebtPayment> findByDebtId(String debtId, String userId) {
        String sql = "SELECT id, debt_id, user_id, payment_amount, payment_date, notes, created_at FROM debt_payments WHERE debt_id = ? AND user_id = ? ORDER BY payment_date DESC, created_at DESC";
        List<DebtPayment> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, debtId);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<DebtPayment> findByUserAndDateRange(String userId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT id, debt_id, user_id, payment_amount, payment_date, notes, created_at FROM debt_payments WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (from != null) {
            sql.append(" AND payment_date >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND payment_date <= ?");
            params.add(to);
        }
        sql.append(" ORDER BY payment_date DESC, created_at DESC");

        List<DebtPayment> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof LocalDate) ps.setObject(i + 1, (LocalDate) p);
                else ps.setString(i + 1, (String) p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Optional<DebtPayment> findByIdAndUser(String id, String userId) {
        String sql = "SELECT id, debt_id, user_id, payment_amount, payment_date, notes, created_at FROM debt_payments WHERE id = ? AND user_id = ?";
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

    public DebtPayment insert(DebtPayment dp) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO debt_payments (id, debt_id, user_id, payment_amount, payment_date, notes) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, dp.getDebtId());
            ps.setString(3, dp.getUserId());
            ps.setBigDecimal(4, dp.getPaymentAmount());
            ps.setObject(5, dp.getPaymentDate());
            ps.setString(6, dp.getNotes());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, dp.getUserId()).orElseThrow();
    }

    public boolean delete(String id, String userId) {
        String sql = "DELETE FROM debt_payments WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal sumByDebt(String debtId) {
        String sql = "SELECT COALESCE(SUM(payment_amount), 0) FROM debt_payments WHERE debt_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, debtId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return BigDecimal.ZERO;
    }

    private static DebtPayment mapRow(ResultSet rs) throws SQLException {
        DebtPayment dp = new DebtPayment();
        dp.setId(rs.getString("id"));
        dp.setDebtId(rs.getString("debt_id"));
        dp.setUserId(rs.getString("user_id"));
        dp.setPaymentAmount(rs.getBigDecimal("payment_amount"));
        Date d = rs.getDate("payment_date");
        dp.setPaymentDate(d != null ? d.toLocalDate() : null);
        dp.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("created_at");
        dp.setCreatedAt(ts != null ? ts.toInstant() : null);
        return dp;
    }
}
