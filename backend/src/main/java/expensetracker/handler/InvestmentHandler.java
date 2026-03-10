package expensetracker.handler;

import com.google.gson.Gson;
import expensetracker.dao.InvestmentDao;
import expensetracker.model.Investment;
import expensetracker.model.User;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InvestmentHandler {
    private static final Gson gson = new Gson();
    private final InvestmentDao investmentDao = new InvestmentDao();

    public void list(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String fromStr = ctx.queryParam("from");
        String toStr = ctx.queryParam("to");
        String type = ctx.queryParam("type");
        LocalDate from = fromStr != null && !fromStr.isBlank() ? LocalDate.parse(fromStr) : null;
        LocalDate to = toStr != null && !toStr.isBlank() ? LocalDate.parse(toStr) : null;
        List<Investment> list = investmentDao.findByUserAndFilters(user.getId(), from, to, type);
        ctx.json(list.stream().map(this::entryResponse).collect(Collectors.toList()));
    }

    public void getOne(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String id = ctx.pathParam("id");
        var opt = investmentDao.findByIdAndUser(id, user.getId());
        if (opt.isEmpty()) {
            ctx.status(404).json(Map.of("error", "Investment not found"));
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
            Investment inv = new Investment();
            inv.setUserId(user.getId());
            inv.setType(required(body, "type"));
            inv.setName(required(body, "name"));
            inv.setInvestedAmount(new BigDecimal(required(body, "investedAmount").toString()));
            inv.setCurrentValue(new BigDecimal(required(body, "currentValue").toString()));
            inv.setEntryDate(LocalDate.parse(required(body, "entryDate").toString()));
            inv.setUnits(body.containsKey("units") && body.get("units") != null ? new BigDecimal(body.get("units").toString()) : null);
            inv.setNavPrice(body.containsKey("navPrice") && body.get("navPrice") != null ? new BigDecimal(body.get("navPrice").toString()) : null);
            inv.setNotes(body.containsKey("notes") && body.get("notes") != null ? body.get("notes").toString() : null);
            Investment created = investmentDao.insert(inv);
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
            String type = required(body, "type");
            String name = required(body, "name");
            BigDecimal investedAmount = new BigDecimal(required(body, "investedAmount").toString());
            BigDecimal currentValue = new BigDecimal(required(body, "currentValue").toString());
            LocalDate entryDate = LocalDate.parse(required(body, "entryDate").toString());
            BigDecimal units = body.containsKey("units") && body.get("units") != null ? new BigDecimal(body.get("units").toString()) : null;
            BigDecimal navPrice = body.containsKey("navPrice") && body.get("navPrice") != null ? new BigDecimal(body.get("navPrice").toString()) : null;
            String notes = body.containsKey("notes") && body.get("notes") != null ? body.get("notes").toString() : null;
            var updated = investmentDao.update(id, user.getId(), type, name, investedAmount, currentValue, units, navPrice, entryDate, notes);
            if (updated.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Investment not found"));
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
        if (!investmentDao.delete(id, user.getId())) {
            ctx.status(404).json(Map.of("error", "Investment not found"));
            return;
        }
        ctx.status(204);
    }

    public void portfolio(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        List<Investment> allInvestments = investmentDao.findByUserAndFilters(user.getId(), null, null, null);

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        Map<String, BigDecimal[]> typeMap = new HashMap<>();

        for (Investment inv : allInvestments) {
            totalInvested = totalInvested.add(inv.getInvestedAmount());
            totalCurrentValue = totalCurrentValue.add(inv.getCurrentValue());

            String type = inv.getType();
            if (!typeMap.containsKey(type)) {
                typeMap.put(type, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            }
            BigDecimal[] values = typeMap.get(type);
            values[0] = values[0].add(inv.getInvestedAmount());
            values[1] = values[1].add(inv.getCurrentValue());
        }

        BigDecimal gainLoss = totalCurrentValue.subtract(totalInvested);
        BigDecimal gainLossPercent = totalInvested.compareTo(BigDecimal.ZERO) > 0
            ? gainLoss.divide(totalInvested, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        final BigDecimal finalTotalInvested = totalInvested;
        List<Map<String, Object>> allocationByType = typeMap.entrySet().stream().map(entry -> {
            String type = entry.getKey();
            BigDecimal[] values = entry.getValue();
            BigDecimal invested = values[0];
            BigDecimal cv = values[1];
            BigDecimal percentage = finalTotalInvested.compareTo(BigDecimal.ZERO) > 0
                ? invested.divide(finalTotalInvested, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

            Map<String, Object> typeEntry = new HashMap<>();
            typeEntry.put("type", type);
            typeEntry.put("label", type);
            typeEntry.put("invested", invested);
            typeEntry.put("currentValue", cv);
            typeEntry.put("percentage", percentage);
            return typeEntry;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("totalInvested", totalInvested);
        response.put("totalCurrentValue", totalCurrentValue);
        response.put("gainLoss", gainLoss);
        response.put("gainLossPercent", gainLossPercent);
        response.put("allocationByType", allocationByType);
        ctx.json(response);
    }

    private String required(Map<String, Object> body, String key) {
        if (!body.containsKey(key) || body.get(key) == null || body.get(key).toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return body.get(key).toString();
    }

    private Map<String, Object> entryResponse(Investment inv) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", inv.getId());
        m.put("type", inv.getType());
        m.put("name", inv.getName());
        m.put("investedAmount", inv.getInvestedAmount());
        m.put("currentValue", inv.getCurrentValue());
        m.put("units", inv.getUnits());
        m.put("navPrice", inv.getNavPrice());
        m.put("entryDate", inv.getEntryDate() != null ? inv.getEntryDate().toString() : null);
        m.put("notes", inv.getNotes());
        m.put("createdAt", inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : null);
        return m;
    }
}
