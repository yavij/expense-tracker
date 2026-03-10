package expensetracker.dao;

import expensetracker.model.Investment;
import expensetracker.util.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class InvestmentDao {

    public List<Investment> findByUserAndFilters(String userId, LocalDate from, LocalDate to, String type) {
        StringBuilder sql = new StringBuilder("SELECT id, user_id, type, name, invested_amount, current_value, units, nav_price, entry_date, notes, created_at FROM investments WHERE user_id = ?");
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
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        sql.append(" ORDER BY entry_date DESC, created_at DESC");

        List<Investment> list = new ArrayList<>();
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

    public Optional<Investment> findByIdAndUser(String id, String userId) {
        String sql = "SELECT id, user_id, type, name, invested_amount, current_value, units, nav_price, entry_date, notes, created_at FROM investments WHERE id = ? AND user_id = ?";
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

    public Investment insert(Investment i) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO investments (id, user_id, type, name, invested_amount, current_value, units, nav_price, entry_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, i.getUserId());
            ps.setString(3, i.getType());
            ps.setString(4, i.getName());
            ps.setBigDecimal(5, i.getInvestedAmount());
            ps.setBigDecimal(6, i.getCurrentValue());
            ps.setObject(7, i.getUnits());
            ps.setObject(8, i.getNavPrice());
            ps.setObject(9, i.getEntryDate());
            ps.setString(10, i.getNotes());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, i.getUserId()).orElseThrow();
    }

    public Optional<Investment> update(String id, String userId, String type, String name, BigDecimal investedAmount, BigDecimal currentValue, BigDecimal units, BigDecimal navPrice, LocalDate entryDate, String notes) {
        String sql = "UPDATE investments SET type = ?, name = ?, invested_amount = ?, current_value = ?, units = ?, nav_price = ?, entry_date = ?, notes = ? WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, name);
            ps.setBigDecimal(3, investedAmount);
            ps.setBigDecimal(4, currentValue);
            ps.setObject(5, units);
            ps.setObject(6, navPrice);
            ps.setObject(7, entryDate);
            ps.setString(8, notes);
            ps.setString(9, id);
            ps.setString(10, userId);
            if (ps.executeUpdate() == 0) return Optional.empty();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return findByIdAndUser(id, userId);
    }

    public boolean delete(String id, String userId) {
        String sql = "DELETE FROM investments WHERE id = ? AND user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal sumByUserAndType(String userId, String type) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(current_value), 0) FROM investments WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (type != null && !type.isBlank()) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                ps.setString(i + 1, (String) p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal sumInvestedByUser(String userId) {
        String sql = "SELECT COALESCE(SUM(invested_amount), 0) FROM investments WHERE user_id = ?";
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

    private static Investment mapRow(ResultSet rs) throws SQLException {
        Investment i = new Investment();
        i.setId(rs.getString("id"));
        i.setUserId(rs.getString("user_id"));
        i.setType(rs.getString("type"));
        i.setName(rs.getString("name"));
        i.setInvestedAmount(rs.getBigDecimal("invested_amount"));
        i.setCurrentValue(rs.getBigDecimal("current_value"));
        i.setUnits(rs.getBigDecimal("units"));
        i.setNavPrice(rs.getBigDecimal("nav_price"));
        Date d = rs.getDate("entry_date");
        i.setEntryDate(d != null ? d.toLocalDate() : null);
        i.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("created_at");
        i.setCreatedAt(ts != null ? ts.toInstant() : null);
        return i;
    }
}
