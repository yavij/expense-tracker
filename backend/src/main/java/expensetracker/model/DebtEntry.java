package expensetracker.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class DebtEntry {
    public static final String HOME_LOAN = "HOME_LOAN";
    public static final String CAR_LOAN = "CAR_LOAN";
    public static final String PERSONAL_LOAN = "PERSONAL_LOAN";
    public static final String EDUCATION_LOAN = "EDUCATION_LOAN";
    public static final String CREDIT_CARD = "CREDIT_CARD";

    public static final String ACTIVE = "ACTIVE";
    public static final String PAID_OFF = "PAID_OFF";
    public static final String PAUSED = "PAUSED";

    private String id;
    private String userId;
    private String name;
    private String type;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private BigDecimal emiAmount;
    private BigDecimal remainingBalance;
    private LocalDate startDate;
    private LocalDate endDate;
    private int priority;
    private String status;
    private Instant createdAt;

    public DebtEntry() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public BigDecimal getEmiAmount() { return emiAmount; }
    public void setEmiAmount(BigDecimal emiAmount) { this.emiAmount = emiAmount; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
