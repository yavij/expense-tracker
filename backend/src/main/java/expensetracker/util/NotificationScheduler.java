package expensetracker.util;

import expensetracker.dao.BudgetDao;
import expensetracker.dao.DebtDao;
import expensetracker.dao.ExpenseDao;
import expensetracker.dao.SalaryDao;
import expensetracker.dao.UserDao;
import expensetracker.model.Budget;
import expensetracker.model.DebtEntry;
import expensetracker.model.ExpenseEntry;
import expensetracker.model.SalaryEntry;
import expensetracker.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Notification scheduler for Expense Tracker.
 * Schedules and manages periodic notification tasks such as:
 * - Budget alerts (daily check if spending exceeds threshold)
 * - Debt EMI reminders (daily check for upcoming EMI due dates)
 * - Weekly summaries (weekly expense and income overview)
 */
public class NotificationScheduler {
    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final ScheduledExecutorService scheduler;
    private final EmailService emailService;
    private final BudgetDao budgetDao;
    private final DebtDao debtDao;
    private final ExpenseDao expenseDao;
    private final SalaryDao salaryDao;
    private final UserDao userDao;

    public NotificationScheduler() {
        this.scheduler = new ScheduledThreadPoolExecutor(2);
        this.emailService = new EmailService();
        this.budgetDao = new BudgetDao();
        this.debtDao = new DebtDao();
        this.expenseDao = new ExpenseDao();
        this.salaryDao = new SalaryDao();
        this.userDao = new UserDao();
    }

    /**
     * Start scheduling notification tasks.
     * - Budget alert check: runs daily at startup
     * - Debt EMI reminder: runs daily
     */
    public void start() {
        log.info("Starting notification scheduler");

        // Schedule budget alert check - runs immediately and then every 24 hours
        scheduler.scheduleAtFixedRate(
                this::checkBudgetAlerts,
                0,
                1,
                TimeUnit.DAYS
        );

        // Schedule debt EMI reminder check - runs immediately and then every 24 hours
        scheduler.scheduleAtFixedRate(
                this::checkDebtReminders,
                0,
                1,
                TimeUnit.DAYS
        );

        log.info("Notification scheduler started with daily budget and debt checks");
    }

    /**
     * Stop the notification scheduler.
     */
    public void stop() {
        log.info("Stopping notification scheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Notification scheduler stopped");
    }

    /**
     * Check all budgets and send alerts if spending exceeds threshold.
     */
    private void checkBudgetAlerts() {
        try {
            log.debug("Running budget alert check");

            // Get all users
            List<User> users = userDao.findAll();
            String currentMonth = YearMonth.now().toString(); // Format: "2026-03"

            for (User user : users) {
                try {
                    // Get user's budgets for current month
                    List<Budget> budgets = budgetDao.findByUserAndMonth(user.getId(), currentMonth);

                    for (Budget budget : budgets) {
                        if (budget.getCategory() == null || budget.getCategory().isBlank()) {
                            continue; // Skip overall budgets
                        }

                        // Calculate spending for this category in current month
                        LocalDate monthStart = LocalDate.of(YearMonth.now().getYear(), YearMonth.now().getMonth(), 1);
                        LocalDate monthEnd = LocalDate.of(YearMonth.now().getYear(), YearMonth.now().getMonth(),
                                YearMonth.now().getMonth().length(YearMonth.now().getYear() == 2025));

                        List<ExpenseEntry> expenses = expenseDao.findByUserAndFilters(
                                user.getId(),
                                monthStart,
                                monthEnd,
                                budget.getCategory()
                        );

                        double totalSpent = expenses.stream()
                                .mapToDouble(e -> e.getAmount().doubleValue())
                                .sum();

                        double monthlyLimit = budget.getMonthlyLimit();
                        double percentage = (totalSpent / monthlyLimit) * 100;

                        // Check if spending exceeds alert threshold
                        if (percentage >= budget.getAlertThreshold()) {
                            log.info("Budget alert for user {}, category: {}, spent: {}, limit: {}, percentage: {}%",
                                    user.getId(), budget.getCategory(), totalSpent, monthlyLimit, percentage);

                            emailService.sendBudgetAlert(
                                    user.getEmail(),
                                    budget.getCategory(),
                                    totalSpent,
                                    monthlyLimit,
                                    percentage
                            );
                        }
                    }
                } catch (Exception e) {
                    log.error("Error checking budget alerts for user {}", user.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in budget alert check", e);
        }
    }

    /**
     * Check all debts and send reminders for EMI due within 3 days.
     */
    private void checkDebtReminders() {
        try {
            log.debug("Running debt reminder check");

            // Get all users
            List<User> users = userDao.findAll();
            LocalDate today = LocalDate.now();
            LocalDate threeDaysLater = today.plusDays(3);

            for (User user : users) {
                try {
                    // Get all active debts for user
                    List<DebtEntry> debts = debtDao.findByUserAndStatus(user.getId(), DebtEntry.ACTIVE);

                    for (DebtEntry debt : debts) {
                        if (debt.getEndDate() == null) {
                            continue; // Skip if no end date
                        }

                        // Check if EMI is due within 3 days
                        // Note: A simple check - in production you might track actual payment dates
                        if (!debt.getEndDate().isBefore(today) && !debt.getEndDate().isAfter(threeDaysLater)) {
                            log.info("Debt reminder for user {}, debt: {}, due date: {}",
                                    user.getId(), debt.getName(), debt.getEndDate());

                            emailService.sendDebtReminder(
                                    user.getEmail(),
                                    debt.getName(),
                                    debt.getEmiAmount().doubleValue(),
                                    debt.getEndDate()
                            );
                        }
                    }
                } catch (Exception e) {
                    log.error("Error checking debt reminders for user {}", user.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error in debt reminder check", e);
        }
    }

    /**
     * Manual method to check and send weekly summaries.
     * Can be scheduled separately if needed.
     */
    public void sendWeeklySummaries() {
        try {
            log.debug("Sending weekly summaries");

            List<User> users = userDao.findAll();
            LocalDate weekStart = LocalDate.now().minusDays(7);
            LocalDate weekEnd = LocalDate.now();

            for (User user : users) {
                try {
                    // Get expenses for the week
                    List<ExpenseEntry> expenses = expenseDao.findByUserAndFilters(
                            user.getId(),
                            weekStart,
                            weekEnd,
                            null
                    );

                    double totalExpenses = expenses.stream()
                            .mapToDouble(e -> e.getAmount().doubleValue())
                            .sum();

                    // Get salary entries for the week
                    List<SalaryEntry> salaries = salaryDao.findByUserAndDateRange(user.getId(), weekStart, weekEnd);
                    double totalIncome = salaries.stream()
                            .mapToDouble(s -> s.getNetAmount().doubleValue())
                            .sum();

                    double savings = totalIncome - totalExpenses;

                    log.info("Weekly summary for user {}: income={}, expenses={}, savings={}",
                            user.getId(), totalIncome, totalExpenses, savings);

                    emailService.sendWeeklySummary(
                            user.getEmail(),
                            totalExpenses,
                            totalIncome,
                            savings
                    );
                } catch (Exception e) {
                    log.error("Error sending weekly summary for user {}", user.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error sending weekly summaries", e);
        }
    }
}
