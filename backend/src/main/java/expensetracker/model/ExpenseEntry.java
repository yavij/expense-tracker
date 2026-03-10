package expensetracker.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class ExpenseEntry {
    public static final String LOAN_PERSONAL = "LOAN_PERSONAL";
    public static final String LOAN_OFFICE = "LOAN_OFFICE";
    public static final String SAVINGS = "SAVINGS";
    public static final String DAILY = "DAILY";
    public static final String HOME = "HOME";
    public static final String COSMETICS = "COSMETICS";
    public static final String TRIP = "TRIP";

    private String id;
    private String userId;
    private String category;
    private BigDecimal amount;
    private String currency;
    private LocalDate entryDate;
    private String note;
    private String loanName;
    private int version = 1;
    private Instant createdAt;

    public ExpenseEntry() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getLoanName() { return loanName; }
    public void setLoanName(String loanName) { this.loanName = loanName; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
