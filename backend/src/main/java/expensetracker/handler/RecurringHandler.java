package expensetracker.handler;

import com.google.gson.Gson;
import expensetracker.dao.ExpenseDao;
import expensetracker.dao.RecurringTransactionDao;
import expensetracker.model.ExpenseEntry;
import expensetracker.model.RecurringTransaction;
import expensetracker.model.User;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecurringHandler {
    private static final Gson gson = new Gson();
    private final RecurringTransactionDao recurringDao = new RecurringTransactionDao();
    private final ExpenseDao expenseDao = new ExpenseDao();

    public void list(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        List<RecurringTransaction> list = recurringDao.findByUser(user.getId());
        ctx.json(list.stream().map(this::txResponse).collect(Collectors.toList()));
    }

    public void getOne(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String id = ctx.pathParam("id");
        var opt = recurringDao.findByIdAndUser(id, user.getId());
        if (opt.isEmpty()) {
            ctx.status(404).json(Map.of("error", "Recurring transaction not found"));
            return;
        }
        ctx.json(txResponse(opt.get()));
    }

    public void create(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body == null) body = new HashMap<>();
            RecurringTransaction tx = new RecurringTransaction();
            tx.setUserId(user.getId());
            tx.setName(required(body, "name"));
            tx.setCategory(required(body, "category"));
            tx.setAmount(new BigDecimal(required(body, "amount").toString()));
            tx.setCurrency(body.containsKey("currency") && body.get("currency") != null ? body.get("currency").toString() : "INR");
            tx.setFrequency(required(body, "frequency"));
            tx.setNextDueDate(LocalDate.parse(required(body, "nextDueDate").toString()));
            tx.setActive(body.containsKey("isActive") && body.get("isActive") != null ? (boolean) body.get("isActive") : true);
            RecurringTransaction created = recurringDao.create(tx);
            ctx.status(201).json(txResponse(created));
        } catch (IllegalArgumentException ex) {
            ctx.status(400).json(Map.of("error", ex.getMessage()));
        }
    }

    public void update(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        try {
            String id = ctx.pathParam("id");
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body == null) body = new HashMap<>();
            String name = required(body, "name");
            String category = required(body, "category");
            BigDecimal amount = new BigDecimal(required(body, "amount").toString());
            String currency = body.containsKey("currency") && body.get("currency") != null ? body.get("currency").toString() : "INR";
            String frequency = required(body, "frequency");
            LocalDate nextDueDate = LocalDate.parse(required(body, "nextDueDate").toString());
            boolean isActive = body.containsKey("isActive") && body.get("isActive") != null ? (boolean) body.get("isActive") : true;
            var updated = recurringDao.update(id, user.getId(), name, category, amount, currency, frequency, nextDueDate, isActive);
            if (updated.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Recurring transaction not found"));
                return;
            }
            ctx.json(txResponse(updated.get()));
        } catch (IllegalArgumentException ex) {
            ctx.status(400).json(Map.of("error", ex.getMessage()));
        }
    }

    public void delete(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String id = ctx.pathParam("id");
        if (!recurringDao.delete(id, user.getId())) {
            ctx.status(404).json(Map.of("error", "Recurring transaction not found"));
            return;
        }
        ctx.status(204);
    }

    public void processDue(Context ctx) {
        try {
            LocalDate today = LocalDate.now();
            List<RecurringTransaction> dueTransactions = recurringDao.findDueTransactions(today);

            Map<String, Object> result = new HashMap<>();
            int processed = 0;

            for (RecurringTransaction tx : dueTransactions) {
                // Create expense entry for this recurring transaction
                ExpenseEntry expense = new ExpenseEntry();
                expense.setUserId(tx.getUserId());
                expense.setCategory(tx.getCategory());
                expense.setAmount(tx.getAmount());
                expense.setCurrency(tx.getCurrency());
                expense.setEntryDate(tx.getNextDueDate());
                expense.setNote("Auto-generated from recurring transaction: " + tx.getName());
                expenseDao.insert(expense);

                // Calculate next due date based on frequency
                LocalDate nextDue = calculateNextDueDate(tx.getNextDueDate(), tx.getFrequency());
                recurringDao.updateNextDueDate(tx.getId(), nextDue);
                processed++;
            }

            result.put("processed", processed);
            result.put("nextRunDate", today.toString());
            ctx.json(result);
        } catch (Exception ex) {
            ctx.status(500).json(Map.of("error", "Failed to process due transactions: " + ex.getMessage()));
        }
    }

    private LocalDate calculateNextDueDate(LocalDate currentDate, String frequency) {
        switch (frequency) {
            case RecurringTransaction.FREQUENCY_DAILY:
                return currentDate.plusDays(1);
            case RecurringTransaction.FREQUENCY_WEEKLY:
                return currentDate.plusWeeks(1);
            case RecurringTransaction.FREQUENCY_MONTHLY:
                return currentDate.plusMonths(1);
            case RecurringTransaction.FREQUENCY_YEARLY:
                return currentDate.plusYears(1);
            default:
                return currentDate;
        }
    }

    private String required(Map<String, Object> body, String key) {
        if (!body.containsKey(key) || body.get(key) == null || body.get(key).toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return body.get(key).toString();
    }

    private Map<String, Object> txResponse(RecurringTransaction tx) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", tx.getId());
        m.put("name", tx.getName());
        m.put("category", tx.getCategory());
        m.put("amount", tx.getAmount());
        m.put("currency", tx.getCurrency());
        m.put("frequency", tx.getFrequency());
        m.put("nextDueDate", tx.getNextDueDate() != null ? tx.getNextDueDate().toString() : null);
        m.put("isActive", tx.isActive());
        m.put("createdAt", tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null);
        return m;
    }
}
