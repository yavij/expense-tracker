package expensetracker.handler;

import com.google.gson.Gson;
import expensetracker.dao.DebtDao;
import expensetracker.dao.DebtPaymentDao;
import expensetracker.model.DebtEntry;
import expensetracker.model.DebtPayment;
import expensetracker.model.User;
import expensetracker.util.DebtCalculator;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DebtHandler {
    private static final Gson gson = new Gson();
    private final DebtDao debtDao = new DebtDao();
    private final DebtPaymentDao paymentDao = new DebtPaymentDao();

    public void list(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String status = ctx.queryParam("status");
        List<DebtEntry> list = debtDao.findByUserAndStatus(user.getId(), status);
        ctx.json(list.stream().map(this::entryResponse).collect(Collectors.toList()));
    }

    public void getOne(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String id = ctx.pathParam("id");
        var opt = debtDao.findByIdAndUser(id, user.getId());
        if (opt.isEmpty()) {
            ctx.status(404).json(Map.of("error", "Debt not found"));
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
            DebtEntry debt = new DebtEntry();
            debt.setUserId(user.getId());
            debt.setName(required(body, "name"));
            debt.setType(required(body, "type"));
            debt.setPrincipalAmount(new BigDecimal(required(body, "principalAmount").toString()));
            debt.setInterestRate(new BigDecimal(required(body, "interestRate").toString()));
            debt.setEmiAmount(new BigDecimal(required(body, "emiAmount").toString()));
            debt.setRemainingBalance(new BigDecimal(required(body, "remainingBalance").toString()));
            debt.setStartDate(LocalDate.parse(required(body, "startDate").toString()));
            debt.setPriority(Integer.parseInt(required(body, "priority")));
            debt.setStatus(required(body, "status"));
            debt.setEndDate(body.containsKey("endDate") && body.get("endDate") != null ? LocalDate.parse(body.get("endDate").toString()) : null);
            DebtEntry created = debtDao.insert(debt);
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
            String name = required(body, "name");
            String type = required(body, "type");
            BigDecimal principalAmount = new BigDecimal(required(body, "principalAmount").toString());
            BigDecimal interestRate = new BigDecimal(required(body, "interestRate").toString());
            BigDecimal emiAmount = new BigDecimal(required(body, "emiAmount").toString());
            BigDecimal remainingBalance = new BigDecimal(required(body, "remainingBalance").toString());
            LocalDate startDate = LocalDate.parse(required(body, "startDate").toString());
            int priority = Integer.parseInt(required(body, "priority"));
            String status = required(body, "status");
            LocalDate endDate = body.containsKey("endDate") && body.get("endDate") != null ? LocalDate.parse(body.get("endDate").toString()) : null;
            var updated = debtDao.update(id, user.getId(), name, type, principalAmount, interestRate, emiAmount, remainingBalance, startDate, endDate, priority, status);
            if (updated.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Debt not found"));
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
        if (!debtDao.delete(id, user.getId())) {
            ctx.status(404).json(Map.of("error", "Debt not found"));
            return;
        }
        ctx.status(204);
    }

    public void addPayment(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        try {
            String debtId = ctx.pathParam("id");
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body == null) body = new HashMap<>();

            var debtOpt = debtDao.findByIdAndUser(debtId, user.getId());
            if (debtOpt.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Debt not found"));
                return;
            }

            DebtEntry debt = debtOpt.get();
            BigDecimal paymentAmount = new BigDecimal(required(body, "paymentAmount").toString());
            LocalDate paymentDate = LocalDate.parse(required(body, "paymentDate").toString());
            String notes = body.containsKey("notes") && body.get("notes") != null ? body.get("notes").toString() : null;

            DebtPayment payment = new DebtPayment();
            payment.setDebtId(debtId);
            payment.setUserId(user.getId());
            payment.setPaymentAmount(paymentAmount);
            payment.setPaymentDate(paymentDate);
            payment.setNotes(notes);
            DebtPayment created = paymentDao.insert(payment);

            // Update remaining balance
            BigDecimal newBalance = debt.getRemainingBalance().subtract(paymentAmount);
            debtDao.updateBalance(debtId, user.getId(), newBalance);

            Map<String, Object> response = new HashMap<>();
            response.put("payment", paymentResponse(created));
            response.put("debtRemainingBalance", newBalance);
            ctx.status(201).json(response);
        } catch (IllegalArgumentException ex) {
            ctx.status(400).json(Map.of("error", ex.getMessage()));
        }
    }

    public void listPayments(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String debtId = ctx.pathParam("id");
        var debtOpt = debtDao.findByIdAndUser(debtId, user.getId());
        if (debtOpt.isEmpty()) {
            ctx.status(404).json(Map.of("error", "Debt not found"));
            return;
        }
        List<DebtPayment> payments = paymentDao.findByDebtId(debtId, user.getId());
        ctx.json(payments.stream().map(this::paymentResponse).collect(Collectors.toList()));
    }

    public void payoffSchedule(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String strategy = ctx.queryParam("strategy") != null ? ctx.queryParam("strategy") : "SNOWBALL";
        String extraStr = ctx.queryParam("extraPayment");
        BigDecimal extraPayment = extraStr != null && !extraStr.isBlank() ? new BigDecimal(extraStr) : BigDecimal.ZERO;

        List<DebtEntry> activeDebts = debtDao.findByUserAndStatus(user.getId(), "ACTIVE");

        if (strategy.equals("SNOWBALL")) {
            activeDebts.sort((a, b) -> a.getRemainingBalance().compareTo(b.getRemainingBalance()));
        } else if (strategy.equals("AVALANCHE")) {
            activeDebts.sort((a, b) -> b.getInterestRate().compareTo(a.getInterestRate()));
        }

        // Calculate total minimum EMI
        BigDecimal totalEmi = activeDebts.stream()
            .map(DebtEntry::getEmiAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyPayment = totalEmi.add(extraPayment);

        List<Map<String, Object>> schedule = DebtCalculator.calculatePayoffSchedule(activeDebts, monthlyPayment, strategy);
        int totalMonths = schedule.size();

        BigDecimal totalInterestPaid = BigDecimal.ZERO;
        for (Map<String, Object> entry : schedule) {
            if (entry.containsKey("interest")) {
                totalInterestPaid = totalInterestPaid.add((BigDecimal) entry.get("interest"));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("strategy", strategy);
        response.put("monthlyPayment", monthlyPayment);
        response.put("extraPayment", extraPayment);
        response.put("schedule", schedule);
        response.put("totalMonths", totalMonths);
        response.put("totalInterestPaid", totalInterestPaid);
        ctx.json(response);
    }

    private String required(Map<String, Object> body, String key) {
        if (!body.containsKey(key) || body.get(key) == null || body.get(key).toString().isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return body.get(key).toString();
    }

    private Map<String, Object> entryResponse(DebtEntry debt) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", debt.getId());
        m.put("name", debt.getName());
        m.put("type", debt.getType());
        m.put("principalAmount", debt.getPrincipalAmount());
        m.put("interestRate", debt.getInterestRate());
        m.put("emiAmount", debt.getEmiAmount());
        m.put("remainingBalance", debt.getRemainingBalance());
        m.put("startDate", debt.getStartDate() != null ? debt.getStartDate().toString() : null);
        m.put("endDate", debt.getEndDate() != null ? debt.getEndDate().toString() : null);
        m.put("priority", debt.getPriority());
        m.put("status", debt.getStatus());
        m.put("createdAt", debt.getCreatedAt() != null ? debt.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> paymentResponse(DebtPayment payment) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", payment.getId());
        m.put("debtId", payment.getDebtId());
        m.put("paymentAmount", payment.getPaymentAmount());
        m.put("paymentDate", payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : null);
        m.put("notes", payment.getNotes());
        m.put("createdAt", payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null);
        return m;
    }
}
