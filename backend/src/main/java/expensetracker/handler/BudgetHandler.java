package expensetracker.handler;

import com.google.gson.Gson;
import expensetracker.dao.BudgetDao;
import expensetracker.dao.ExpenseDao;
import expensetracker.model.Budget;
import expensetracker.model.User;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BudgetHandler {
    private static final Gson gson = new Gson();
    private final BudgetDao budgetDao = new BudgetDao();
    private final ExpenseDao expenseDao = new ExpenseDao();

    public void list(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        List<Budget> budgets = budgetDao.findByUser(user.getId());
        ctx.json(budgets.stream().map(this::budgetResponse).collect(Collectors.toList()));
    }

    public void status(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String monthStr = ctx.queryParam("month");
        YearMonth currentMonth;
        if (monthStr != null && !monthStr.isBlank()) {
            currentMonth = YearMonth.parse(monthStr);
        } else {
            currentMonth = YearMonth.now();
        }

        LocalDate from = currentMonth.atDay(1);
        LocalDate to = currentMonth.atEndOfMonth();

        List<Budget> budgets = budgetDao.findByUserAndMonth(user.getId(), currentMonth.toString());

        // Build status for each budget
        List<Map<String, Object>> statusList = budgets.stream()
                .map(budget -> buildBudgetStatus(budget, from, to))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("month", currentMonth.toString());
        response.put("budgets", statusList);
        ctx.json(response);
    }

    public void upsert(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body == null) body = new HashMap<>();

            Budget budget = new Budget();
            budget.setUserId(user.getId());

            // If id provided, it's an update
            if (body.containsKey("id") && body.get("id") != null) {
                budget.setId(body.get("id").toString());
            } else {
                budget.setId(null); // Will be generated
            }

            budget.setCategory(body.containsKey("category") && body.get("category") != null ? body.get("category").toString() : null);
            budget.setMonthlyLimit(Double.parseDouble(required(body, "monthlyLimit").toString()));
            budget.setAlertThreshold(Integer.parseInt(body.containsKey("alertThreshold") ? body.get("alertThreshold").toString() : "80"));
            budget.setMonth(body.containsKey("month") && body.get("month") != null ? body.get("month").toString() : null);

            Budget created = budgetDao.upsert(budget);
            ctx.json(budgetResponse(created));
        } catch (IllegalArgumentException ex) {
            ctx.status(400).json(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            ctx.status(400).json(Map.of("error", "Invalid request: " + ex.getMessage()));
        }
    }

    public void delete(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String id = ctx.pathParam("id");
        if (!budgetDao.delete(id, user.getId())) {
            ctx.status(404).json(Map.of("error", "Budget not found"));
            return;
        }
        ctx.status(204);
    }

    private Map<String, Object> buildBudgetStatus(Budget budget, LocalDate from, LocalDate to) {
        Map<String, Object> status = new HashMap<>();
        status.put("id", budget.getId());
        status.put("category", budget.getCategory());
        status.put("monthlyLimit", budget.getMonthlyLimit());
        status.put("alertThreshold", budget.getAlertThreshold());
        status.put("month", budget.getMonth());

        // Get actual spending
        BigDecimal actualSpending = expenseDao.sumByUserAndCategoryAndDateRange(
                budget.getUserId(),
                budget.getCategory(),
                from,
                to
        );
        status.put("actualSpending", actualSpending);

        double spent = actualSpending.doubleValue();
        double limit = budget.getMonthlyLimit();
        double percentageUsed = limit > 0 ? (spent / limit) * 100 : 0;
        status.put("percentageUsed", Math.round(percentageUsed * 100.0) / 100.0);

        boolean alertTriggered = percentageUsed >= budget.getAlertThreshold();
        status.put("alertTriggered", alertTriggered);
        status.put("remainingBudget", Math.max(0, limit - spent));

        return status;
    }

    private Map<String, Object> budgetResponse(Budget b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", b.getId());
        m.put("category", b.getCategory());
        m.put("monthlyLimit", b.getMonthlyLimit());
        m.put("alertThreshold", b.getAlertThreshold());
        m.put("month", b.getMonth());
        m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
        return m;
    }

    private String required(Map<String, Object> body, String key) {
        if (!body.containsKey(key) || body.get(key) == null || body.get(key).toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return body.get(key).toString();
    }
}
