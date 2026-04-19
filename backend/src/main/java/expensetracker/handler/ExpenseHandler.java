package expensetracker.handler;

import com.google.gson.Gson;
import expensetracker.dao.ExpenseDao;
import expensetracker.exception.ConflictException;
import expensetracker.model.ExpenseEntry;
import expensetracker.model.User;
import expensetracker.util.ValidationUtil;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExpenseHandler {
    private static final Gson gson = new Gson();
    private final ExpenseDao expenseDao = new ExpenseDao();

    // Valid expense categories
    private static final String[] VALID_CATEGORIES = {
            ExpenseEntry.LOAN_PERSONAL,
            ExpenseEntry.LOAN_OFFICE,
            ExpenseEntry.SAVINGS,
            ExpenseEntry.DAILY,
            ExpenseEntry.HOME,
            ExpenseEntry.COSMETICS,
            ExpenseEntry.TRIP
    };

    public void list(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String fromStr = ctx.queryParam("from");
        String toStr = ctx.queryParam("to");
        String category = ctx.queryParam("category");
        LocalDate from = fromStr != null && !fromStr.isBlank() ? LocalDate.parse(fromStr) : null;
        LocalDate to = toStr != null && !toStr.isBlank() ? LocalDate.parse(toStr) : null;
        List<ExpenseEntry> allEntries = expenseDao.findByUserAndFilters(user.getId(), from, to, category);

        String limitStr = ctx.queryParam("limit");
        String offsetStr = ctx.queryParam("offset");
        if (limitStr != null && !limitStr.isBlank()) {
            int limit = Math.min(Integer.parseInt(limitStr), 500);
            int offset = (offsetStr != null && !offsetStr.isBlank()) ? Integer.parseInt(offsetStr) : 0;
            int total = allEntries.size();
            List<ExpenseEntry> page = allEntries.subList(
                    Math.min(offset, total),
                    Math.min(offset + limit, total)
            );
            Map<String, Object> result = new HashMap<>();
            result.put("data", page.stream().map(this::entryResponse).collect(Collectors.toList()));
            result.put("total", total);
            result.put("limit", limit);
            result.put("offset", offset);
            ctx.json(result);
        } else {
            ctx.json(allEntries.stream().map(this::entryResponse).collect(Collectors.toList()));
        }
    }

    public void search(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String query = ctx.queryParam("q");
        if (query == null || query.isBlank()) {
            ctx.json(List.of());
            return;
        }
        List<ExpenseEntry> list = expenseDao.searchByUser(user.getId(), query);
        ctx.json(list.stream().map(this::entryResponse).collect(Collectors.toList()));
    }

    public void getOne(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String id = ctx.pathParam("id");
        var opt = expenseDao.findByIdAndUser(id, user.getId());
        if (opt.isEmpty()) {
            ctx.status(404).json(Map.of("error", "Expense not found"));
            return;
        }
        ctx.json(entryResponse(opt.get()));
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

            // Validate category
            String category = ValidationUtil.validateCategory(
                    required(body, "category"),
                    VALID_CATEGORIES
            );

            // Validate amount
            BigDecimal amount = ValidationUtil.validateAmount(body.get("amount"));

            // Validate date
            LocalDate entryDate = ValidationUtil.validateDate(required(body, "date"));

            // Validate currency
            String currency = ValidationUtil.validateCurrency(
                    body.containsKey("currency") ? body.get("currency").toString() : "INR"
            );

            // Validate and sanitize note
            String note = null;
            if (body.containsKey("note") && body.get("note") != null) {
                String noteStr = body.get("note").toString();
                if (!noteStr.isBlank()) {
                    note = ValidationUtil.sanitizeString(noteStr, 500);
                }
            }

            // Validate and sanitize loanName
            String loanName = null;
            if (body.containsKey("loanName") && body.get("loanName") != null) {
                String loanNameStr = body.get("loanName").toString();
                if (!loanNameStr.isBlank()) {
                    loanName = ValidationUtil.sanitizeString(loanNameStr, 100);
                }
            }

            ExpenseEntry e = new ExpenseEntry();
            e.setUserId(user.getId());
            e.setCategory(category);
            e.setAmount(amount);
            e.setCurrency(currency);
            e.setEntryDate(entryDate);
            e.setNote(note);
            e.setLoanName(loanName);

            ExpenseEntry created = expenseDao.insert(e);
            ctx.status(201).json(entryResponse(created));
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

            // Validate category
            String category = ValidationUtil.validateCategory(
                    required(body, "category"),
                    VALID_CATEGORIES
            );

            // Validate amount
            BigDecimal amount = ValidationUtil.validateAmount(body.get("amount"));

            // Validate date
            LocalDate entryDate = ValidationUtil.validateDate(required(body, "date"));

            // Validate currency
            String currency = ValidationUtil.validateCurrency(
                    body.containsKey("currency") ? body.get("currency").toString() : "INR"
            );

            // Validate and sanitize note
            String note = null;
            if (body.containsKey("note") && body.get("note") != null) {
                String noteStr = body.get("note").toString();
                if (!noteStr.isBlank()) {
                    note = ValidationUtil.sanitizeString(noteStr, 500);
                }
            }

            // Validate and sanitize loanName
            String loanName = null;
            if (body.containsKey("loanName") && body.get("loanName") != null) {
                String loanNameStr = body.get("loanName").toString();
                if (!loanNameStr.isBlank()) {
                    loanName = ValidationUtil.sanitizeString(loanNameStr, 100);
                }
            }

            // Check for version parameter for optimistic locking
            if (body.containsKey("version")) {
                int version = Integer.parseInt(body.get("version").toString());
                try {
                    var updated = expenseDao.updateWithVersion(id, user.getId(), category, amount, currency, entryDate, note, loanName, version);
                    if (updated.isEmpty()) {
                        ctx.status(404).json(Map.of("error", "Expense not found"));
                        return;
                    }
                    ctx.json(entryResponse(updated.get()));
                } catch (ConflictException ex) {
                    ctx.status(409).json(Map.of("error", ex.getMessage()));
                }
            } else {
                var updated = expenseDao.update(id, user.getId(), category, amount, currency, entryDate, note, loanName);
                if (updated.isEmpty()) {
                    ctx.status(404).json(Map.of("error", "Expense not found"));
                    return;
                }
                ctx.json(entryResponse(updated.get()));
            }
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
        if (!expenseDao.delete(id, user.getId())) {
            ctx.status(404).json(Map.of("error", "Expense not found"));
            return;
        }
        ctx.status(204);
    }

    public void summary(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String month = ctx.queryParam("month"); // yyyy-MM
        LocalDate from;
        LocalDate to;
        if (month != null && !month.isBlank()) {
            from = LocalDate.parse(month + "-01");
            to = from.withDayOfMonth(from.lengthOfMonth());
        } else {
            from = LocalDate.now().withDayOfMonth(1);
            to = LocalDate.now();
        }
        BigDecimal savings = expenseDao.sumByUserAndCategoryAndDateRange(user.getId(), ExpenseEntry.SAVINGS, from, to);
        BigDecimal loans = expenseDao.sumByUserAndCategoryAndDateRange(user.getId(), ExpenseEntry.LOAN_PERSONAL, from, to)
                .add(expenseDao.sumByUserAndCategoryAndDateRange(user.getId(), ExpenseEntry.LOAN_OFFICE, from, to));
        BigDecimal expenses = expenseDao.sumByUserAndCategoryAndDateRange(user.getId(), ExpenseEntry.DAILY, from, to)
                .add(expenseDao.sumByUserAndCategoryAndDateRange(user.getId(), ExpenseEntry.HOME, from, to))
                .add(expenseDao.sumByUserAndCategoryAndDateRange(user.getId(), ExpenseEntry.COSMETICS, from, to))
                .add(expenseDao.sumByUserAndCategoryAndDateRange(user.getId(), ExpenseEntry.TRIP, from, to));
        Map<String, Object> m = new HashMap<>();
        m.put("savings", savings);
        m.put("loans", loans);
        m.put("expenses", expenses);
        m.put("from", from.toString());
        m.put("to", to.toString());
        ctx.json(m);
    }

    private String required(Map<String, Object> body, String key) {
        if (!body.containsKey(key) || body.get(key) == null || body.get(key).toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return body.get(key).toString();
    }

    private Map<String, Object> entryResponse(ExpenseEntry e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("category", e.getCategory());
        m.put("amount", e.getAmount());
        m.put("currency", e.getCurrency());
        m.put("date", e.getEntryDate() != null ? e.getEntryDate().toString() : null);
        m.put("note", e.getNote());
        m.put("loanName", e.getLoanName());
        m.put("version", e.getVersion());
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        return m;
    }
}
