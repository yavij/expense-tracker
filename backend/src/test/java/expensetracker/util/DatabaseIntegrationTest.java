package expensetracker.util;

import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Database Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseIntegrationTest {

    @BeforeAll
    static void setUp() {
        // Use in-memory H2 for test isolation
        System.setProperty("JDBC_URL", "jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        // Set env via system properties fallback
        if (System.getenv("JDBC_URL") == null) {
            // For tests, manually set the JDBC_URL environment
            // The Database class reads from System.getenv, so we need to ensure H2 in-memory is used
        }
    }

    @Test
    @Order(1)
    @DisplayName("Database.init() should create all tables without errors")
    void initShouldCreateAllTables() {
        assertDoesNotThrow(() -> {
            Database.init();
        }, "Database initialization should not throw exceptions");
    }

    @Test
    @Order(2)
    @DisplayName("Should be able to get a connection from the pool")
    void shouldGetConnection() throws Exception {
        try (Connection conn = Database.getConnection()) {
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
        }
    }

    @Test
    @Order(3)
    @DisplayName("All 11 tables should exist after initialization")
    void allTablesShouldExist() throws Exception {
        String[] expectedTables = {
            "users", "login_events", "expense_entries", "investments",
            "salary_entries", "debt_entries", "debt_payments",
            "user_preferences", "budgets", "recurring_transactions", "subscriptions"
        };

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String table : expectedTables) {
                ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + table.toUpperCase() + "'"
                );
                assertTrue(rs.next(), "Should have result for table: " + table);
                assertTrue(rs.getInt(1) > 0, "Table should exist: " + table);
                rs.close();
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("isH2() should return true for H2 database")
    void isH2ShouldReturnTrue() {
        assertTrue(Database.isH2(), "Should be H2 database in test");
    }

    @Test
    @Order(5)
    @DisplayName("Should be able to insert and query a user")
    void shouldInsertAndQueryUser() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // Insert a test user
            stmt.executeUpdate(
                "INSERT INTO users (id, email, name, role) VALUES " +
                "('test-user-1', 'test@example.com', 'Test User', 'USER')"
            );

            // Query back
            ResultSet rs = stmt.executeQuery(
                "SELECT id, email, name, role FROM users WHERE id = 'test-user-1'"
            );
            assertTrue(rs.next(), "User should exist");
            assertEquals("test@example.com", rs.getString("email"));
            assertEquals("Test User", rs.getString("name"));
            assertEquals("USER", rs.getString("role"));
            rs.close();

            // Clean up
            stmt.executeUpdate("DELETE FROM users WHERE id = 'test-user-1'");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Version column should default to 1 for expense_entries")
    void versionColumnDefaultsToOne() throws Exception {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // Need a user first (FK constraint)
            stmt.executeUpdate(
                "INSERT INTO users (id, email, name, role) VALUES " +
                "('test-user-v', 'version-test@example.com', 'Version Test', 'USER')"
            );

            stmt.executeUpdate(
                "INSERT INTO expense_entries (id, user_id, category, amount, entry_date) VALUES " +
                "('test-expense-1', 'test-user-v', 'DAILY', 100.00, '2026-03-01')"
            );

            ResultSet rs = stmt.executeQuery(
                "SELECT version FROM expense_entries WHERE id = 'test-expense-1'"
            );
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("version"), "Default version should be 1");
            rs.close();

            // Clean up (cascades)
            stmt.executeUpdate("DELETE FROM users WHERE id = 'test-user-v'");
        }
    }
}
