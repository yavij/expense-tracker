package expensetracker.handler;

import com.google.gson.Gson;
import expensetracker.dao.SalaryDao;
import expensetracker.model.SalaryEntry;
import expensetracker.model.User;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SalaryHandler {
    private static final Gson gson = new Gson();
    private final SalaryDao salaryDao = new SalaryDao();

    public void list(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String fromStr = ctx.queryParam("from");
        String toStr = ctx.queryParam("to");
        LocalDate from = fromStr != null && !fromStr.isBlank() ? LocalDate.parse(fromStr) : null;
        LocalDate to = toStr != null && !toStr.isBlank() ? LocalDate.parse(toStr) : null;
        List<SalaryEntry> list = salaryDao.findByUserAndDateRange(user.getId(), from, to);
        ctx.json(list.stream().map(this::entryResponse).collect(Collectors.toList()));
    }

    public void getOne(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String id = ctx.pathParam("id");
        var opt = salaryDao.findByIdAndUser(id, user.getId());
        if (opt.isEmpty()) {
            ctx.status(404).json(Map.of("error", "Salary entry not found"));
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
            SalaryEntry sal = new SalaryEntry();
            sal.setUserId(user.getId());
            // month comes as "2026-01" from frontend; convert to first-of-month LocalDate
            String monthStr = required(body, "month");
            sal.setMonth(parseMonth(monthStr));
            sal.setGrossAmount(new BigDecimal(required(body, "grossAmount").toString()));
            sal.setDeductions(new BigDecimal(required(body, "deductions").toString()));
            sal.setNetAmount(new BigDecimal(required(body, "netAmount").toString()));
            sal.setNotes(body.containsKey("notes") && body.get("notes") != null ? body.get("notes").toString() : null);
            SalaryEntry created = salaryDao.insert(sal);
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
            LocalDate month = parseMonth(required(body, "month"));
            BigDecimal grossAmount = new BigDecimal(required(body, "grossAmount").toString());
            BigDecimal deductions = new BigDecimal(required(body, "deductions").toString());
            BigDecimal netAmount = new BigDecimal(required(body, "netAmount").toString());
            String notes = body.containsKey("notes") && body.get("notes") != null ? body.get("notes").toString() : null;
            var updated = salaryDao.update(id, user.getId(), month, grossAmount, deductions, netAmount, notes);
            if (updated.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Salary entry not found"));
                return;
            }
            ctx.json(entryResponse(updated.get()));
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
        if (!salaryDao.delete(id, user.getId())) {
            ctx.status(404).json(Map.of("error", "Salary entry not found"));
            return;
        }
        ctx.status(204);
    }

    public void history(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        List<SalaryEntry> allSalaries = salaryDao.findByUserAndDateRange(user.getId(), null, null);

        List<Map<String, Object>> entries = allSalaries.stream()
            .map(this::entryResponse)
            .collect(Collectors.toList());

        Map<Integer, BigDecimal[]> yearMap = new HashMap<>();
        for (SalaryEntry sal : allSalaries) {
            int year = sal.getMonth().getYear();
            if (!yearMap.containsKey(year)) {
                yearMap.put(year, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            }
            BigDecimal[] totals = yearMap.get(year);
            totals[0] = totals[0].add(sal.getGrossAmount());
            totals[1] = totals[1].add(sal.getNetAmount());
        }

        List<Map<String, Object>> yearlyTotals = yearMap.entrySet().stream()
            .map(entry -> {
                Map<String, Object> yearEntry = new HashMap<>();
                yearEntry.put("year", entry.getKey());
                yearEntry.put("totalGross", entry.getValue()[0]);
                yearEntry.put("totalNet", entry.getValue()[1]);
                return yearEntry;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("entries", entries);
        response.put("yearlyTotals", yearlyTotals);
        ctx.json(response);
    }

    /** Parse month string: accepts "2026-01" or "2026-01-01" */
    private LocalDate parseMonth(String monthStr) {
        if (monthStr.length() == 7) {
            return LocalDate.parse(monthStr + "-01");
        }
        return LocalDate.parse(monthStr);
    }

    private String required(Map<String, Object> body, String key) {
        if (!body.containsKey(key) || body.get(key) == null || body.get(key).toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return body.get(key).toString();
    }

    private Map<String, Object> entryResponse(SalaryEntry sal) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", sal.getId());
        m.put("month", sal.getMonth() != null ? sal.getMonth().toString() : null);
        m.put("grossAmount", sal.getGrossAmount());
        m.put("deductions", sal.getDeductions());
        m.put("netAmount", sal.getNetAmount());
        m.put("notes", sal.getNotes());
        m.put("createdAt", sal.getCreatedAt() != null ? sal.getCreatedAt().toString() : null);
        return m;
    }
}
