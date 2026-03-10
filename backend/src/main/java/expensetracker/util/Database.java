package expensetracker.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static HikariDataSource ds;
    private static boolean isH2 = false;

    public static void init() {
        String jdbcUrl = System.getenv("JDBC_URL");
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            // Default to H2 file-based database for easy local dev (no MySQL needed)
            jdbcUrl = "jdbc:h2:./expensetracker;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";
            isH2 = true;
            System.out.println("[DB] No JDBC_URL set - using embedded H2 database (./expensetracker.mv.db)");
        } else {
            isH2 = jdbcUrl.startsWith("jdbc:h2:");
            if (jdbcUrl.startsWith("jdbc:mysql:")) {
                System.out.println("[DB] Using MySQL: " + jdbcUrl.replaceAll("password=[^&]*", "password=***"));
            } else {
                System.out.println("[DB] Using: " + jdbcUrl);
            }
        }

        String user = System.getenv("DB_USER");
        if (user == null) user = isH2 ? "sa" : "root";
        String password = System.getenv("DB_PASSWORD");
        if (password == null) password = "";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        ds = new HikariDataSource(config);

        // Auto-create tables on startup
        createSchema();
    }

    private static void createSchema() {
        String usersTable;
        String loginEventsTable;
        String expensesTable;
        String investmentsTable;
        String salaryEntriesTable;
        String debtEntriesTable;
        String debtPaymentsTable;
        String userPreferencesTable;
        String budgetsTable;
        String recurringTransactionsTable;
        String subscriptionsTable;

        if (isH2) {
            usersTable = "CREATE TABLE IF NOT EXISTS users ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "email VARCHAR(255) NOT NULL UNIQUE, "
                    + "name VARCHAR(255), "
                    + "picture_url VARCHAR(512), "
                    + "phone_number VARCHAR(20), "
                    + "role VARCHAR(20) NOT NULL DEFAULT 'USER', "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";
            loginEventsTable = "CREATE TABLE IF NOT EXISTS login_events ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "logged_in_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "ip_address VARCHAR(45), "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            expensesTable = "CREATE TABLE IF NOT EXISTS expense_entries ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "category VARCHAR(32) NOT NULL, "
                    + "amount DECIMAL(15, 2) NOT NULL, "
                    + "currency VARCHAR(10) NOT NULL DEFAULT 'INR', "
                    + "entry_date DATE NOT NULL, "
                    + "note VARCHAR(500), "
                    + "loan_name VARCHAR(255), "
                    + "version INT NOT NULL DEFAULT 1, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            investmentsTable = "CREATE TABLE IF NOT EXISTS investments ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "type VARCHAR(20) NOT NULL, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "invested_amount DECIMAL(15, 2) NOT NULL, "
                    + "current_value DECIMAL(15, 2) NOT NULL, "
                    + "units DECIMAL(15, 6), "
                    + "nav_price DECIMAL(15, 6), "
                    + "entry_date DATE NOT NULL, "
                    + "notes VARCHAR(500), "
                    + "version INT NOT NULL DEFAULT 1, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            salaryEntriesTable = "CREATE TABLE IF NOT EXISTS salary_entries ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "\"month\" DATE NOT NULL, "
                    + "gross_amount DECIMAL(15, 2) NOT NULL, "
                    + "deductions DECIMAL(15, 2) NOT NULL DEFAULT 0, "
                    + "net_amount DECIMAL(15, 2) NOT NULL, "
                    + "notes VARCHAR(500), "
                    + "version INT NOT NULL DEFAULT 1, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            debtEntriesTable = "CREATE TABLE IF NOT EXISTS debt_entries ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "type VARCHAR(32) NOT NULL, "
                    + "principal_amount DECIMAL(15, 2) NOT NULL, "
                    + "interest_rate DECIMAL(5, 2) NOT NULL, "
                    + "emi_amount DECIMAL(15, 2) NOT NULL, "
                    + "remaining_balance DECIMAL(15, 2) NOT NULL, "
                    + "start_date DATE NOT NULL, "
                    + "end_date DATE, "
                    + "priority INT NOT NULL DEFAULT 0, "
                    + "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', "
                    + "version INT NOT NULL DEFAULT 1, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            debtPaymentsTable = "CREATE TABLE IF NOT EXISTS debt_payments ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "debt_id VARCHAR(36) NOT NULL, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "payment_amount DECIMAL(15, 2) NOT NULL, "
                    + "payment_date DATE NOT NULL, "
                    + "notes VARCHAR(500), "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (debt_id) REFERENCES debt_entries(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            userPreferencesTable = "CREATE TABLE IF NOT EXISTS user_preferences ("
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "pref_key VARCHAR(50) NOT NULL, "
                    + "pref_value VARCHAR(500), "
                    + "PRIMARY KEY (user_id, pref_key), "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            budgetsTable = "CREATE TABLE IF NOT EXISTS budgets ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "category VARCHAR(32), "
                    + "monthly_limit DECIMAL(15, 2) NOT NULL, "
                    + "alert_threshold INT NOT NULL DEFAULT 80, "
                    + "\"month\" VARCHAR(7), "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE (user_id, category, \"month\"), "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            recurringTransactionsTable = "CREATE TABLE IF NOT EXISTS recurring_transactions ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "category VARCHAR(32) NOT NULL, "
                    + "amount DECIMAL(15, 2) NOT NULL, "
                    + "currency VARCHAR(10) NOT NULL DEFAULT 'INR', "
                    + "frequency VARCHAR(20) NOT NULL, "
                    + "next_due_date DATE NOT NULL, "
                    + "is_active BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            subscriptionsTable = "CREATE TABLE IF NOT EXISTS subscriptions ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "razorpay_order_id VARCHAR(100), "
                    + "razorpay_payment_id VARCHAR(100), "
                    + "amount DECIMAL(15, 2) NOT NULL, "
                    + "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', "
                    + "activated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "expires_at TIMESTAMP NOT NULL, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
        } else {
            // MySQL schema (original schema.sql)
            usersTable = "CREATE TABLE IF NOT EXISTS users ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "email VARCHAR(255) NOT NULL UNIQUE, "
                    + "name VARCHAR(255), "
                    + "picture_url VARCHAR(512), "
                    + "phone_number VARCHAR(20), "
                    + "role VARCHAR(20) NOT NULL DEFAULT 'USER', "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "INDEX idx_users_email (email)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            loginEventsTable = "CREATE TABLE IF NOT EXISTS login_events ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "logged_in_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "ip_address VARCHAR(45), "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "INDEX idx_login_events_user_id (user_id), "
                    + "INDEX idx_login_events_logged_in_at (logged_in_at)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            expensesTable = "CREATE TABLE IF NOT EXISTS expense_entries ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "category VARCHAR(32) NOT NULL, "
                    + "amount DECIMAL(15, 2) NOT NULL, "
                    + "currency VARCHAR(10) NOT NULL DEFAULT 'INR', "
                    + "entry_date DATE NOT NULL, "
                    + "note VARCHAR(500), "
                    + "loan_name VARCHAR(255), "
                    + "version INT NOT NULL DEFAULT 1, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "INDEX idx_expense_user_date (user_id, entry_date), "
                    + "INDEX idx_expense_category (user_id, category)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            investmentsTable = "CREATE TABLE IF NOT EXISTS investments ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "type VARCHAR(20) NOT NULL, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "invested_amount DECIMAL(15, 2) NOT NULL, "
                    + "current_value DECIMAL(15, 2) NOT NULL, "
                    + "units DECIMAL(15, 6), "
                    + "nav_price DECIMAL(15, 6), "
                    + "entry_date DATE NOT NULL, "
                    + "notes VARCHAR(500), "
                    + "version INT NOT NULL DEFAULT 1, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "INDEX idx_inv_user_type (user_id, type), "
                    + "INDEX idx_inv_user_date (user_id, entry_date)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            salaryEntriesTable = "CREATE TABLE IF NOT EXISTS salary_entries ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "month DATE NOT NULL, "
                    + "gross_amount DECIMAL(15, 2) NOT NULL, "
                    + "deductions DECIMAL(15, 2) NOT NULL DEFAULT 0, "
                    + "net_amount DECIMAL(15, 2) NOT NULL, "
                    + "notes VARCHAR(500), "
                    + "version INT NOT NULL DEFAULT 1, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "INDEX idx_sal_user_month (user_id, month)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            debtEntriesTable = "CREATE TABLE IF NOT EXISTS debt_entries ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "type VARCHAR(32) NOT NULL, "
                    + "principal_amount DECIMAL(15, 2) NOT NULL, "
                    + "interest_rate DECIMAL(5, 2) NOT NULL, "
                    + "emi_amount DECIMAL(15, 2) NOT NULL, "
                    + "remaining_balance DECIMAL(15, 2) NOT NULL, "
                    + "start_date DATE NOT NULL, "
                    + "end_date DATE, "
                    + "priority INT NOT NULL DEFAULT 0, "
                    + "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', "
                    + "version INT NOT NULL DEFAULT 1, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "INDEX idx_debt_user_status (user_id, status), "
                    + "INDEX idx_debt_user_priority (user_id, priority)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            debtPaymentsTable = "CREATE TABLE IF NOT EXISTS debt_payments ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "debt_id VARCHAR(36) NOT NULL, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "payment_amount DECIMAL(15, 2) NOT NULL, "
                    + "payment_date DATE NOT NULL, "
                    + "notes VARCHAR(500), "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (debt_id) REFERENCES debt_entries(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "INDEX idx_dpay_debt_date (debt_id, payment_date), "
                    + "INDEX idx_dpay_user_date (user_id, payment_date)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            userPreferencesTable = "CREATE TABLE IF NOT EXISTS user_preferences ("
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "pref_key VARCHAR(50) NOT NULL, "
                    + "pref_value VARCHAR(500), "
                    + "PRIMARY KEY (user_id, pref_key), "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            budgetsTable = "CREATE TABLE IF NOT EXISTS budgets ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "category VARCHAR(32), "
                    + "monthly_limit DECIMAL(15, 2) NOT NULL, "
                    + "alert_threshold INT NOT NULL DEFAULT 80, "
                    + "month VARCHAR(7), "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE KEY unique_user_category_month (user_id, category, month), "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "INDEX idx_budget_user (user_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            recurringTransactionsTable = "CREATE TABLE IF NOT EXISTS recurring_transactions ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "category VARCHAR(32) NOT NULL, "
                    + "amount DECIMAL(15, 2) NOT NULL, "
                    + "currency VARCHAR(10) NOT NULL DEFAULT 'INR', "
                    + "frequency VARCHAR(20) NOT NULL, "
                    + "next_due_date DATE NOT NULL, "
                    + "is_active BOOLEAN NOT NULL DEFAULT TRUE, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "INDEX idx_recurring_user (user_id), "
                    + "INDEX idx_recurring_due (next_due_date)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            subscriptionsTable = "CREATE TABLE IF NOT EXISTS subscriptions ("
                    + "id VARCHAR(36) PRIMARY KEY, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "razorpay_order_id VARCHAR(100), "
                    + "razorpay_payment_id VARCHAR(100), "
                    + "amount DECIMAL(15, 2) NOT NULL, "
                    + "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', "
                    + "activated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "expires_at TIMESTAMP NOT NULL, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "INDEX idx_sub_user (user_id), "
                    + "INDEX idx_sub_expires (expires_at)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        }

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(usersTable);
            st.execute(loginEventsTable);
            st.execute(expensesTable);
            st.execute(investmentsTable);
            st.execute(salaryEntriesTable);
            st.execute(debtEntriesTable);
            st.execute(debtPaymentsTable);
            st.execute(userPreferencesTable);
            st.execute(budgetsTable);
            st.execute(recurringTransactionsTable);
            st.execute(subscriptionsTable);

            // Create H2 indexes separately (H2 doesn't support inline INDEX in CREATE TABLE)
            if (isH2) {
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_login_events_user_id ON login_events(user_id)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_login_events_logged_in_at ON login_events(logged_in_at)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_expense_user_date ON expense_entries(user_id, entry_date)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_expense_category ON expense_entries(user_id, category)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_inv_user_type ON investments(user_id, type)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_inv_user_date ON investments(user_id, entry_date)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_sal_user_month ON salary_entries(user_id, \"month\")");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_debt_user_status ON debt_entries(user_id, status)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_debt_user_priority ON debt_entries(user_id, priority)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_dpay_debt_date ON debt_payments(debt_id, payment_date)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_dpay_user_date ON debt_payments(user_id, payment_date)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_budget_user ON budgets(user_id)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_recurring_user ON recurring_transactions(user_id)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_recurring_due ON recurring_transactions(next_due_date)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_sub_user ON subscriptions(user_id)");
                safeExecute(st, "CREATE INDEX IF NOT EXISTS idx_sub_expires ON subscriptions(expires_at)");
            }
            System.out.println("[DB] Schema verified / created successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database schema", e);
        }
    }

    private static void safeExecute(Statement st, String sql) {
        try {
            st.execute(sql);
        } catch (SQLException ignored) {
            // Index might already exist
        }
    }

    public static boolean isH2() { return isH2; }

    public static Connection getConnection() throws SQLException {
        if (ds == null) init();
        return ds.getConnection();
    }

    public static void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }
}
