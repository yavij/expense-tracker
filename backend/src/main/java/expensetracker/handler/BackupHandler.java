package expensetracker.handler;

import com.google.gson.Gson;
import expensetracker.dao.*;
import expensetracker.model.*;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

public class BackupHandler {
    private static final Logger log = LoggerFactory.getLogger(BackupHandler.class);
    private static final Gson gson = new Gson();

    private final ExpenseDao expenseDao = new ExpenseDao();
    private final InvestmentDao investmentDao = new InvestmentDao();
    private final SalaryDao salaryDao = new SalaryDao();
    private final DebtDao debtDao = new DebtDao();
    private final DebtPaymentDao debtPaymentDao = new DebtPaymentDao();
    private final RecurringTransactionDao recurringDao = new RecurringTransactionDao();
    private final BudgetDao budgetDao = new BudgetDao();
    private final UserPreferenceDao userPreferenceDao = new UserPreferenceDao();

    /**
     * GET /api/backup - Export all user data as JSON
     * Returns a JSON object with all data grouped by type.
     */
    public void backup(Context ctx) {
        try {
            User user = ctx.attribute("user");
            String userId = user.getId();
            log.info("Backup requested for user: {}", userId);

            Map<String, Object> backup = new HashMap<>();

            // Expenses
            List<ExpenseEntry> expenses = expenseDao.findByUserAndFilters(userId, null, null, null);
            backup.put("expenses", expenses);

            // Investments
            List<Investment> investments = investmentDao.findByUserAndFilters(userId, null, null, null);
            backup.put("investments", investments);

            // Salary entries
            List<SalaryEntry> salaryEntries = salaryDao.findByUserAndDateRange(userId, null, null);
            backup.put("salary_entries", salaryEntries);

            // Debts
            List<DebtEntry> debts = debtDao.findByUserAndStatus(userId, null);
            backup.put("debts", debts);

            // Debt payments
            List<DebtPayment> debtPayments = debtPaymentDao.findByUserAndDateRange(userId, null, null);
            backup.put("debt_payments", debtPayments);

            // Recurring transactions
            List<RecurringTransaction> recurringTransactions = recurringDao.findByUser(userId);
            backup.put("recurring_transactions", recurringTransactions);

            // Budgets
            List<Budget> budgets = budgetDao.findByUser(userId);
            backup.put("budgets", budgets);

            // User preferences
            Map<String, String> preferences = userPreferenceDao.findByUserId(userId);
            backup.put("preferences", preferences);

            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("backup_timestamp", System.currentTimeMillis());
            metadata.put("user_id", userId);
            backup.put("_metadata", metadata);

            log.info("Backup completed successfully for user: {} - {} expenses, {} investments, {} salary entries, {} debts, {} debt payments, {} recurring transactions, {} budgets",
                    userId, expenses.size(), investments.size(), salaryEntries.size(), debts.size(),
                    debtPayments.size(), recurringTransactions.size(), budgets.size());

            ctx.contentType("application/json");
            ctx.json(backup);
        } catch (Exception e) {
            log.error("Backup failed", e);
            ctx.status(500).json(Map.of("error", "Backup failed: " + e.getMessage()));
        }
    }

    /**
     * POST /api/backup/restore - Import data from JSON backup
     * Expects the same format as the backup endpoint.
     * Skips duplicates by ID.
     */
    public void restore(Context ctx) {
        try {
            User user = ctx.attribute("user");
            String userId = user.getId();
            log.info("Restore requested for user: {}", userId);

            Map<String, Object> backup = ctx.bodyAsClass(Map.class);
            if (backup == null) {
                ctx.status(400).json(Map.of("error", "Backup data is required"));
                return;
            }

            Map<String, Integer> counts = new HashMap<>();

            // Restore expenses
            List<Map> expensesList = (List<Map>) backup.getOrDefault("expenses", new ArrayList<>());
            int expenseCount = 0;
            for (Map expenseMap : expensesList) {
                try {
                    ExpenseEntry expense = gson.fromJson(gson.toJson(expenseMap), ExpenseEntry.class);
                    // Only restore if not already exists
                    if (expense.getId() != null && expenseDao.findByIdAndUser(expense.getId(), userId).isEmpty()) {
                        expense.setUserId(userId);
                        expenseDao.insert(expense);
                        expenseCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to restore expense: {}", e.getMessage());
                }
            }
            counts.put("expenses", expenseCount);

            // Restore investments
            List<Map> investmentsList = (List<Map>) backup.getOrDefault("investments", new ArrayList<>());
            int investmentCount = 0;
            for (Map invMap : investmentsList) {
                try {
                    Investment investment = gson.fromJson(gson.toJson(invMap), Investment.class);
                    if (investment.getId() != null && investmentDao.findByIdAndUser(investment.getId(), userId).isEmpty()) {
                        investment.setUserId(userId);
                        investmentDao.insert(investment);
                        investmentCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to restore investment: {}", e.getMessage());
                }
            }
            counts.put("investments", investmentCount);

            // Restore salary entries
            List<Map> salaryList = (List<Map>) backup.getOrDefault("salary_entries", new ArrayList<>());
            int salaryCount = 0;
            for (Map salaryMap : salaryList) {
                try {
                    SalaryEntry salary = gson.fromJson(gson.toJson(salaryMap), SalaryEntry.class);
                    if (salary.getId() != null && salaryDao.findByIdAndUser(salary.getId(), userId).isEmpty()) {
                        salary.setUserId(userId);
                        salaryDao.insert(salary);
                        salaryCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to restore salary entry: {}", e.getMessage());
                }
            }
            counts.put("salary_entries", salaryCount);

            // Restore debts
            List<Map> debtsList = (List<Map>) backup.getOrDefault("debts", new ArrayList<>());
            int debtCount = 0;
            for (Map debtMap : debtsList) {
                try {
                    DebtEntry debt = gson.fromJson(gson.toJson(debtMap), DebtEntry.class);
                    if (debt.getId() != null && debtDao.findByIdAndUser(debt.getId(), userId).isEmpty()) {
                        debt.setUserId(userId);
                        debtDao.insert(debt);
                        debtCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to restore debt: {}", e.getMessage());
                }
            }
            counts.put("debts", debtCount);

            // Restore debt payments
            List<Map> debtPaymentsList = (List<Map>) backup.getOrDefault("debt_payments", new ArrayList<>());
            int debtPaymentCount = 0;
            for (Map paymentMap : debtPaymentsList) {
                try {
                    DebtPayment payment = gson.fromJson(gson.toJson(paymentMap), DebtPayment.class);
                    if (payment.getId() != null && debtPaymentDao.findByIdAndUser(payment.getId(), userId).isEmpty()) {
                        payment.setUserId(userId);
                        debtPaymentDao.insert(payment);
                        debtPaymentCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to restore debt payment: {}", e.getMessage());
                }
            }
            counts.put("debt_payments", debtPaymentCount);

            // Restore recurring transactions
            List<Map> recurringList = (List<Map>) backup.getOrDefault("recurring_transactions", new ArrayList<>());
            int recurringCount = 0;
            for (Map recurringMap : recurringList) {
                try {
                    RecurringTransaction recurring = gson.fromJson(gson.toJson(recurringMap), RecurringTransaction.class);
                    if (recurring.getId() != null && recurringDao.findByIdAndUser(recurring.getId(), userId).isEmpty()) {
                        recurring.setUserId(userId);
                        recurringDao.create(recurring);
                        recurringCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to restore recurring transaction: {}", e.getMessage());
                }
            }
            counts.put("recurring_transactions", recurringCount);

            // Restore budgets
            List<Map> budgetsList = (List<Map>) backup.getOrDefault("budgets", new ArrayList<>());
            int budgetCount = 0;
            for (Map budgetMap : budgetsList) {
                try {
                    Budget budget = gson.fromJson(gson.toJson(budgetMap), Budget.class);
                    if (budget.getId() != null && budgetDao.findByIdAndUser(budget.getId(), userId).isEmpty()) {
                        budget.setUserId(userId);
                        budgetDao.upsert(budget);
                        budgetCount++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to restore budget: {}", e.getMessage());
                }
            }
            counts.put("budgets", budgetCount);

            // Restore preferences
            Map<String, String> preferences = (Map<String, String>) backup.getOrDefault("preferences", new HashMap<>());
            int prefCount = 0;
            for (Map.Entry<String, String> entry : preferences.entrySet()) {
                try {
                    userPreferenceDao.upsert(userId, entry.getKey(), entry.getValue());
                    prefCount++;
                } catch (Exception e) {
                    log.warn("Failed to restore preference: {}", e.getMessage());
                }
            }
            counts.put("preferences", prefCount);

            log.info("Restore completed for user: {} - Restored {} expenses, {} investments, {} salary entries, {} debts, {} debt payments, {} recurring transactions, {} budgets, {} preferences",
                    userId, counts.get("expenses"), counts.get("investments"), counts.get("salary_entries"),
                    counts.get("debts"), counts.get("debt_payments"), counts.get("recurring_transactions"),
                    counts.get("budgets"), counts.get("preferences"));

            ctx.json(Map.of("message", "Data restored successfully", "imported_counts", counts));
        } catch (Exception e) {
            log.error("Restore failed", e);
            ctx.status(500).json(Map.of("error", "Restore failed: " + e.getMessage()));
        }
    }
}
