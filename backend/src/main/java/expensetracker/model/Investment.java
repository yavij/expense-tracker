package expensetracker.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class Investment {
    public static final String MF = "MF";
    public static final String PPF = "PPF";
    public static final String NPS = "NPS";
    public static final String RD = "RD";
    public static final String STOCKS = "STOCKS";

    private String id;
    private String userId;
    private String type;
    private String name;
    private BigDecimal investedAmount;
    private BigDecimal currentValue;
    private BigDecimal units;
    private BigDecimal navPrice;
    private LocalDate entryDate;
    private String notes;
    private Instant createdAt;

    public Investment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getInvestedAmount() { return investedAmount; }
    public void setInvestedAmount(BigDecimal investedAmount) { this.investedAmount = investedAmount; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    public BigDecimal getUnits() { return units; }
    public void setUnits(BigDecimal units) { this.units = units; }
    public BigDecimal getNavPrice() { return navPrice; }
    public void setNavPrice(BigDecimal navPrice) { this.navPrice = navPrice; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
