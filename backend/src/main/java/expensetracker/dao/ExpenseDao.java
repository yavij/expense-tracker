package expensetracker.dao;

import expensetracker.exception.ConflictException;
import expensetracker.model.ExpenseEntry;
import expensetracker.util.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ExpenseDao {

    public List<ExpenseEntry> findByUserAndFilters(String userId, LocalDate from, LocalDate to, String category) {
        StringBuilder sql = new StringBuilder("SELECT id, user_id, category, amount, currency, entry_date, note, loan_name, version, created_at FROM expense_entries WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (from != null) {
            sql.append(" AND entry_date >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND entry_date <= ?");
            params.add(to);
        }
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        sql.append(" ORDER BY entry_date DESC, created_at DESC");

        List<ExpenseEntry> list = new ArrayList<>();
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

    public Optional<ExpenseEntry> findByIdAndUser(String id, String userId) {
        String sql = "SELECT id, user_id, category, amount, currency, entry_date, note, loan_name, version, created_at FROM expense_entries WHERE id = ? AND user_id = ?";
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

    public ExpenseEntry insert(ExpenseEntry e) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO expense_entries (id, user_id, category, amount, currency, entry_date, note, loan_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, e.getUserId());
            ps.setString(3, e.getCategory());
            ps.setBigDecimal(4, e.getAmount());
            ps.setString(5, e.getCurrency() != null ? e.getCurrency() : "INR");
            ps.setObject(6, e.getEntryDate());
            ps.setString(7, e.getNote());
            ps.setString(8, e.getLoanName());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, e.getUserId()).orElseThrow();
    }

    public Optional<ExpenseEntry> update(String id, String userId, String category, BigDecimal amount, String currency, LocalDate entryDate, String note, String loanName) {
        String sql = "UPDATE expense_entries SET category = ?, amount = ?, currency = ?, entry_date = ?, note = ?, loan_name = ? WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setBigDecimal(2, amount);
            ps.setString(3, currency != null ? currency : "INR");
            ps.setObject(4, entryDate);
            ps.setString(5, note);
            ps.setString(6, loanName);
            ps.setString(7, id);
            ps.setString(8, userId);
            if (ps.executeUpdate() == 0) return Optional.empty();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, userId);
    }

    public Optional<ExpenseEntry> updateWithVersion(String id, String userId, String category, BigDecimal amount, String currency, LocalDate entryDate, String note, String loanName, int version) throws ConflictException {
        String sql = "UPDATE expense_entries SET category = ?, amount = ?, currency = ?, entry_date = ?, note = ?, loan_name = ?, version = version + 1 WHERE id = ? AND user_id = ? AND version = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setBigDecimal(2, amount);
            ps.setString(3, currency != null ? currency : "INR");
            ps.setObject(4, entryDate);
            ps.setString(5, note);
            ps.setString(6, loanName);
            ps.setString(7, id);
            ps.setString(8, userId);
            ps.setInt(9, version);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new ConflictException("Version mismatch or expense not found");
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, userId);
    }

    public boolean delete(String id, String userId) {
        String sql = "DELETE FROM expense_entries WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal sumByUserAndDateRange(String userId, LocalDate from, LocalDate to) {
        return sumByUserAndCategoryAndDateRange(userId, null, from, to);
    }

    public BigDecimal sumByUserAndCategoryAndDateRange(String userId, String category, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(amount), 0) FROM expense_entries WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (from != null) {
            sql.append(" AND entry_date >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND entry_date <= ?");
            params.add(to);
        }
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof LocalDate) ps.setObject(i + 1, (LocalDate) p);
                else ps.setString(i + 1, (String) p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return BigDecimal.ZERO;
    }

    public List<ExpenseEntry> searchByUser(String userId, String query) {
        String sql = "SELECT id, user_id, category, amount, currency, entry_date, note, loan_name, version, created_at FROM expense_entries WHERE user_id = ? AND (note LIKE ? OR category LIKE ? OR loan_name LIKE ?) ORDER BY entry_date DESC, created_at DESC";
        List<ExpenseEntry> list = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            ps.setString(1, userId);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setString(4, searchPattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private static ExpenseEntry mapRow(ResultSet rs) throws SQLException {
        ExpenseEntry e = new ExpenseEntry();
        e.setId(rs.getString("id"));
        e.setUserId(rs.getString("user_id"));
        e.setCategory(rs.getString("category"));
        e.setAmount(rs.getBigDecimal("amount"));
        e.setCurrency(rs.getString("currency"));
        Date d = rs.getDate("entry_date");
        e.setEntryDate(d != null ? d.toLocalDate() : null);
        e.setNote(rs.getString("note"));
        e.setLoanName(rs.getString("loan_name"));
        e.setVersion(rs.getInt("version"));
        Timestamp ts = rs.getTimestamp("created_at");
        e.setCreatedAt(ts != null ? ts.toInstant() : null);
        return e;
    }
}
