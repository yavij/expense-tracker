package expensetracker.dao;

import expensetracker.model.User;
import expensetracker.util.Database;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class UserDao {

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, email, name, picture_url, phone_number, role, created_at FROM users WHERE email = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
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

    public Optional<User> findByPhone(String phone) {
        String sql = "SELECT id, email, name, picture_url, phone_number, role, created_at FROM users WHERE phone_number = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
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

    public Optional<User> findById(String id) {
        String sql = "SELECT id, email, name, picture_url, phone_number, role, created_at FROM users WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
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

    public long count() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection c = Database.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public User insert(String email, String name, String pictureUrl, String role) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO users (id, email, name, picture_url, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, email);
            ps.setString(3, name);
            ps.setString(4, pictureUrl);
            ps.setString(5, role != null ? role : "USER");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return findByEmail(email).orElseThrow();
    }

    public User insertWithPhone(String email, String name, String pictureUrl, String role, String phone) {
        String id = UUID.randomUUID().toString();
        String sql = "INSERT INTO users (id, email, name, picture_url, role, phone_number) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, email);
            ps.setString(3, name);
            ps.setString(4, pictureUrl);
            ps.setString(5, role != null ? role : "USER");
            ps.setString(6, phone);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return findByPhone(phone).orElseThrow();
    }

    public void updatePhone(String userId, String phone) {
        String sql = "UPDATE users SET phone_number = ? WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setEmail(rs.getString("email"));
        u.setName(rs.getString("name"));
        u.setPictureUrl(rs.getString("picture_url"));
        u.setPhoneNumber(rs.getString("phone_number"));
        u.setRole(rs.getString("role"));
        Timestamp ts = rs.getTimestamp("created_at");
        u.setCreatedAt(ts != null ? ts.toInstant() : null);
        return u;
    }

    public java.util.List<User> findAll() {
        String sql = "SELECT id, email, name, picture_url, phone_number, role, created_at FROM users ORDER BY created_at DESC";
        java.util.List<User> list = new java.util.ArrayList<>();
        try (Connection c = Database.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
