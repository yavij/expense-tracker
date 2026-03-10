package expensetracker.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class DebtPayment {
    private String id;
    private String debtId;
    private String userId;
    private BigDecimal paymentAmount;
    private LocalDate paymentDate;
    private String notes;
    private Instant createdAt;

    public DebtPayment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDebtId() { return debtId; }
    public void setDebtId(String debtId) { this.debtId = debtId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
