package expensetracker.dao;

import expensetracker.model.SalaryEntry;
import expensetracker.util.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SalaryDao {

    private static String m() { return Database.isH2() ? "\"month\"" : "month"; }

    public List<SalaryEntry> findByUserAndDateRange(String userId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT id, user_id, " + m() + ", gross_amount, deductions, net_amount, notes, created_at FROM salary_entries WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (from != null) {
            sql.append(" AND " + m() + " >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND " + m() + " <= ?");
            params.add(to);
        }
        sql.append(" ORDER BY " + m() + " DESC, created_at DESC");

        List<SalaryEntry> list = new ArrayList<>();
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

    public Optional<SalaryEntry> findByIdAndUser(String id, String userId) {
        String sql = "SELECT id, user_id, " + m() + ", gross_amount, deductions, net_amount, notes, created_at FROM salary_entries WHERE id = ? AND user_id = ?";
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

    public SalaryEntry insert(SalaryEntry s) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO salary_entries (id, user_id, " + m() + ", gross_amount, deductions, net_amount, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, s.getUserId());
            ps.setObject(3, s.getMonth());
            ps.setBigDecimal(4, s.getGrossAmount());
            ps.setBigDecimal(5, s.getDeductions());
            ps.setBigDecimal(6, s.getNetAmount());
            ps.setString(7, s.getNotes());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, s.getUserId()).orElseThrow();
    }

    public Optional<SalaryEntry> update(String id, String userId, LocalDate month, BigDecimal grossAmount, BigDecimal deductions, BigDecimal netAmount, String notes) {
        String sql = "UPDATE salary_entries SET " + m() + " = ?, gross_amount = ?, deductions = ?, net_amount = ?, notes = ? WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, month);
            ps.setBigDecimal(2, grossAmount);
            ps.setBigDecimal(3, deductions);
            ps.setBigDecimal(4, netAmount);
            ps.setString(5, notes);
            ps.setString(6, id);
            ps.setString(7, userId);
            if (ps.executeUpdate() == 0) return Optional.empty();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, userId);
    }

    public boolean delete(String id, String userId) {
        String sql = "DELETE FROM salary_entries WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal sumNetByUserAndDateRange(String userId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(net_amount), 0) FROM salary_entries WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (from != null) {
            sql.append(" AND " + m() + " >= ?");
            params.add(from);
        }
        if (to != null) {
            sql.append(" AND " + m() + " <= ?");
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

    public Optional<SalaryEntry> getLatestByUser(String userId) {
        String sql = "SELECT id, user_id, " + m() + ", gross_amount, deductions, net_amount, notes, created_at FROM salary_entries WHERE user_id = ? ORDER BY " + m() + " DESC LIMIT 1";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    private static SalaryEntry mapRow(ResultSet rs) throws SQLException {
        SalaryEntry s = new SalaryEntry();
        s.setId(rs.getString("id"));
        s.setUserId(rs.getString("user_id"));
        Date d = rs.getDate("month");
        s.setMonth(d != null ? d.toLocalDate() : null);
        s.setGrossAmount(rs.getBigDecimal("gross_amount"));
        s.setDeductions(rs.getBigDecimal("deductions"));
        s.setNetAmount(rs.getBigDecimal("net_amount"));
        s.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("created_at");
        s.setCreatedAt(ts != null ? ts.toInstant() : null);
        return s;
    }
}
