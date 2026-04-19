-- Expense Tracker - MySQL initialization script
-- This script runs automatically when the MySQL Docker container starts for the first time.
-- The application also auto-creates tables on startup via Database.java,
-- but this ensures the schema exists even before the app connects.

CREATE DATABASE IF NOT EXISTS expensetracker;
USE expensetracker;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    picture_url VARCHAR(512),
    phone_number VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Login events
CREATE TABLE IF NOT EXISTS login_events (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    logged_in_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_login_events_user_id (user_id),
    INDEX idx_login_events_logged_in_at (logged_in_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Expense entries
CREATE TABLE IF NOT EXISTS expense_entries (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    category VARCHAR(32) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    entry_date DATE NOT NULL,
    note VARCHAR(500),
    loan_name VARCHAR(255),
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_expense_user_date (user_id, entry_date),
    INDEX idx_expense_category (user_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Investments
CREATE TABLE IF NOT EXISTS investments (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    invested_amount DECIMAL(15, 2) NOT NULL,
    current_value DECIMAL(15, 2) NOT NULL,
    units DECIMAL(15, 6),
    nav_price DECIMAL(15, 6),
    entry_date DATE NOT NULL,
    notes VARCHAR(500),
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_inv_user_type (user_id, type),
    INDEX idx_inv_user_date (user_id, entry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Salary entries
CREATE TABLE IF NOT EXISTS salary_entries (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    month DATE NOT NULL,
    gross_amount DECIMAL(15, 2) NOT NULL,
    deductions DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(15, 2) NOT NULL,
    notes VARCHAR(500),
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_sal_user_month (user_id, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Debt entries
CREATE TABLE IF NOT EXISTS debt_entries (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    principal_amount DECIMAL(15, 2) NOT NULL,
    interest_rate DECIMAL(5, 2) NOT NULL,
    emi_amount DECIMAL(15, 2) NOT NULL,
    remaining_balance DECIMAL(15, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    priority INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_debt_user_status (user_id, status),
    INDEX idx_debt_user_priority (user_id, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Debt payments
CREATE TABLE IF NOT EXISTS debt_payments (
    id VARCHAR(36) PRIMARY KEY,
    debt_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    payment_amount DECIMAL(15, 2) NOT NULL,
    payment_date DATE NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (debt_id) REFERENCES debt_entries(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_dpay_debt_date (debt_id, payment_date),
    INDEX idx_dpay_user_date (user_id, payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- User preferences
CREATE TABLE IF NOT EXISTS user_preferences (
    user_id VARCHAR(36) NOT NULL,
    pref_key VARCHAR(50) NOT NULL,
    pref_value VARCHAR(500),
    PRIMARY KEY (user_id, pref_key),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Budgets
CREATE TABLE IF NOT EXISTS budgets (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    category VARCHAR(32),
    monthly_limit DECIMAL(15, 2) NOT NULL,
    alert_threshold INT NOT NULL DEFAULT 80,
    month VARCHAR(7),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_category_month (user_id, category, month),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_budget_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Recurring transactions
CREATE TABLE IF NOT EXISTS recurring_transactions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(32) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    frequency VARCHAR(20) NOT NULL,
    next_due_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_recurring_user (user_id),
    INDEX idx_recurring_due (next_due_date, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Subscriptions (Razorpay payments)
CREATE TABLE IF NOT EXISTS subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    razorpay_order_id VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    activated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_sub_user (user_id),
    INDEX idx_sub_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
