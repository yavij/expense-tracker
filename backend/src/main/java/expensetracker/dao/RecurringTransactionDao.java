package expensetracker.dao;

import expensetracker.model.RecurringTransaction;
import expensetracker.util.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RecurringTransactionDao {

    public List<RecurringTransaction> findByUser(String userId) {
        String sql = "SELECT id, user_id, name, category, amount, currency, frequency, next_due_date, is_active, created_at FROM recurring_transactions WHERE user_id = ? ORDER BY created_at DESC";
        List<RecurringTransaction> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Optional<RecurringTransaction> findById(String id) {
        String sql = "SELECT id, user_id, name, category, amount, currency, frequency, next_due_date, is_active, created_at FROM recurring_transactions WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public Optional<RecurringTransaction> findByIdAndUser(String id, String userId) {
        String sql = "SELECT id, user_id, name, category, amount, currency, frequency, next_due_date, is_active, created_at FROM recurring_transactions WHERE id = ? AND user_id = ?";
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

    public RecurringTransaction create(RecurringTransaction tx) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO recurring_transactions (id, user_id, name, category, amount, currency, frequency, next_due_date, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, tx.getUserId());
            ps.setString(3, tx.getName());
            ps.setString(4, tx.getCategory());
            ps.setBigDecimal(5, tx.getAmount());
            ps.setString(6, tx.getCurrency() != null ? tx.getCurrency() : "INR");
            ps.setString(7, tx.getFrequency());
            ps.setObject(8, tx.getNextDueDate());
            ps.setBoolean(9, tx.isActive());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findById(id).orElseThrow();
    }

    public Optional<RecurringTransaction> update(String id, String userId, String name, String category, BigDecimal amount, String currency, String frequency, LocalDate nextDueDate, boolean isActive) {
        String sql = "UPDATE recurring_transactions SET name = ?, category = ?, amount = ?, currency = ?, frequency = ?, next_due_date = ?, is_active = ? WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setBigDecimal(3, amount);
            ps.setString(4, currency != null ? currency : "INR");
            ps.setString(5, frequency);
            ps.setObject(6, nextDueDate);
            ps.setBoolean(7, isActive);
            ps.setString(8, id);
            ps.setString(9, userId);
            if (ps.executeUpdate() == 0) return Optional.empty();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, userId);
    }

    public boolean delete(String id, String userId) {
        String sql = "DELETE FROM recurring_transactions WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<RecurringTransaction> findDueTransactions(LocalDate date) {
        String sql = "SELECT id, user_id, name, category, amount, currency, frequency, next_due_date, is_active, created_at FROM recurring_transactions WHERE is_active = TRUE AND next_due_date <= ?";
        List<RecurringTransaction> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Optional<RecurringTransaction> updateNextDueDate(String id, LocalDate nextDueDate) {
        String sql = "UPDATE recurring_transactions SET next_due_date = ? WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, nextDueDate);
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findById(id);
    }

    private static RecurringTransaction mapRow(ResultSet rs) throws SQLException {
        RecurringTransaction tx = new RecurringTransaction();
        tx.setId(rs.getString("id"));
        tx.setUserId(rs.getString("user_id"));
        tx.setName(rs.getString("name"));
        tx.setCategory(rs.getString("category"));
        tx.setAmount(rs.getBigDecimal("amount"));
        tx.setCurrency(rs.getString("currency"));
        tx.setFrequency(rs.getString("frequency"));
        Date d = rs.getDate("next_due_date");
        tx.setNextDueDate(d != null ? d.toLocalDate() : null);
        tx.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        tx.setCreatedAt(ts != null ? ts.toInstant() : null);
        return tx;
    }
}
