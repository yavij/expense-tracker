package expensetracker.model;

import java.time.Instant;

public class Budget {
    private String id;
    private String userId;
    private String category;          // nullable for overall budget
    private double monthlyLimit;
    private int alertThreshold;       // percentage like 80
    private String month;             // like "2026-03" or null for recurring
    private Instant createdAt;

    public Budget() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }

    public int getAlertThreshold() { return alertThreshold; }
    public void setAlertThreshold(int alertThreshold) { this.alertThreshold = alertThreshold; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
