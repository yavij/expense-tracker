package expensetracker.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * Email notification service for Expense Tracker.
 * Currently logs emails to console/logs. Can be extended with actual SMTP integration.
 */
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final boolean enabled;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUser;
    private final String smtpPassword;

    public EmailService() {
        this.enabled = Boolean.parseBoolean(System.getenv().getOrDefault("EMAIL_ENABLED", "false"));
        this.smtpHost = System.getenv().getOrDefault("SMTP_HOST", "smtp.gmail.com");
        this.smtpPort = Integer.parseInt(System.getenv().getOrDefault("SMTP_PORT", "587"));
        this.smtpUser = System.getenv().getOrDefault("SMTP_USER", "");
        this.smtpPassword = System.getenv().getOrDefault("SMTP_PASSWORD", "");

        if (enabled) {
            log.info("Email service initialized with SMTP: {}:{}", smtpHost, smtpPort);
        } else {
            log.info("Email service disabled. Emails will be logged only.");
        }
    }

    /**
     * Send budget alert email when spending exceeds threshold.
     *
     * @param email        recipient email address
     * @param categoryName category name
     * @param spent        amount spent
     * @param limit        budget limit
     * @param percentage   percentage of budget used
     */
    public void sendBudgetAlert(String email, String categoryName, double spent, double limit, double percentage) {
        String subject = "Budget Alert: " + categoryName + " Category";
        String body = buildBudgetAlertBody(categoryName, spent, limit, percentage);

        sendEmail(email, subject, body);
    }

    /**
     * Send debt EMI reminder email.
     *
     * @param email       recipient email address
     * @param debtName    name of the debt
     * @param emiAmount   EMI amount due
     * @param dueDate     due date of the EMI
     */
    public void sendDebtReminder(String email, String debtName, double emiAmount, LocalDate dueDate) {
        String subject = "Debt EMI Reminder: " + debtName;
        String body = buildDebtReminderBody(debtName, emiAmount, dueDate);

        sendEmail(email, subject, body);
    }

    /**
     * Send weekly summary email with expense and income overview.
     *
     * @param email           recipient email address
     * @param totalExpenses   total expenses for the week
     * @param totalIncome     total income for the week
     * @param savings         total savings for the week
     */
    public void sendWeeklySummary(String email, double totalExpenses, double totalIncome, double savings) {
        String subject = "Weekly Financial Summary";
        String body = buildWeeklySummaryBody(totalExpenses, totalIncome, savings);

        sendEmail(email, subject, body);
    }

    /**
     * Internal method to send email.
     *
     * @param email   recipient email address
     * @param subject email subject
     * @param body    email body
     */
    private void sendEmail(String email, String subject, String body) {
        if (!enabled) {
            log.info("Email (disabled) would be sent to: {}", email);
            log.debug("Subject: {}", subject);
            log.debug("Body:\n{}", body);
            return;
        }

        // TODO: Implement actual SMTP integration
        log.info("Sending email to: {}", email);
        log.info("Subject: {}", subject);
        log.debug("Body:\n{}", body);
    }

    /**
     * Build budget alert email body.
     */
    private String buildBudgetAlertBody(String categoryName, double spent, double limit, double percentage) {
        return String.format(
                "Hello,\n\n" +
                        "This is a budget alert notification.\n\n" +
                        "Category: %s\n" +
                        "Amount Spent: ₹%.2f\n" +
                        "Budget Limit: ₹%.2f\n" +
                        "Percentage Used: %.1f%%\n\n" +
                        "You are exceeding your budget for this category. Please review your spending.\n\n" +
                        "Best regards,\n" +
                        "Expense Tracker Team",
                categoryName, spent, limit, percentage
        );
    }

    /**
     * Build debt EMI reminder email body.
     */
    private String buildDebtReminderBody(String debtName, double emiAmount, LocalDate dueDate) {
        return String.format(
                "Hello,\n\n" +
                        "This is a debt EMI reminder.\n\n" +
                        "Debt: %s\n" +
                        "EMI Amount Due: ₹%.2f\n" +
                        "Due Date: %s\n\n" +
                        "Please ensure timely payment to avoid penalties.\n\n" +
                        "Best regards,\n" +
                        "Expense Tracker Team",
                debtName, emiAmount, dueDate
        );
    }

    /**
     * Build weekly summary email body.
     */
    private String buildWeeklySummaryBody(double totalExpenses, double totalIncome, double savings) {
        return String.format(
                "Hello,\n\n" +
                        "Here is your weekly financial summary.\n\n" +
                        "Total Income: ₹%.2f\n" +
                        "Total Expenses: ₹%.2f\n" +
                        "Savings: ₹%.2f\n\n" +
                        "Keep track of your spending and stay within your budget.\n\n" +
                        "Best regards,\n" +
                        "Expense Tracker Team",
                totalIncome, totalExpenses, savings
        );
    }

    public boolean isEnabled() {
        return enabled;
    }
}
