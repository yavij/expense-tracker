package expensetracker.handler;

import com.google.gson.Gson;
import expensetracker.dao.DebtDao;
import expensetracker.dao.DebtPaymentDao;
import expensetracker.dao.ExpenseDao;
import expensetracker.dao.InvestmentDao;
import expensetracker.dao.SalaryDao;
import expensetracker.model.DebtEntry;
import expensetracker.model.DebtPayment;
import expensetracker.model.ExpenseEntry;
import expensetracker.model.Investment;
import expensetracker.model.SalaryEntry;
import expensetracker.model.User;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalyticsHandler {
    private static final Gson gson = new Gson();
    private final ExpenseDao expenseDao = new ExpenseDao();
    private final InvestmentDao investmentDao = new InvestmentDao();
    private final SalaryDao salaryDao = new SalaryDao();
    private final DebtDao debtDao = new DebtDao();
    private final DebtPaymentDao debtPaymentDao = new DebtPaymentDao();

    public void monthly(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String monthStr = ctx.queryParam("month");
        YearMonth month = monthStr != null && !monthStr.isBlank() ? YearMonth.parse(monthStr) : YearMonth.now();

        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        List<SalaryEntry> salaries = salaryDao.findByUserAndDateRange(user.getId(), from, to);
        BigDecimal grossSalary = BigDecimal.ZERO;
        BigDecimal netSalary = BigDecimal.ZERO;
        for (SalaryEntry sal : salaries) {
            grossSalary = grossSalary.add(sal.getGrossAmount());
            netSalary = netSalary.add(sal.getNetAmount());
        }

        BigDecimal totalExpenses = expenseDao.sumByUserAndDateRange(user.getId(), from, to);

        List<Investment> investments = investmentDao.findByUserAndFilters(user.getId(), from, to, null);
        BigDecimal totalInvested = investments.stream()
            .map(Investment::getInvestedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DebtPayment> debtPayments = debtPaymentDao.findByUserAndDateRange(user.getId(), from, to);
        BigDecimal totalDebtPaid = debtPayments.stream()
            .map(DebtPayment::getPaymentAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netChange = netSalary.subtract(totalExpenses).subtract(totalDebtPaid);

        List<ExpenseEntry> expenseEntries = expenseDao.findByUserAndFilters(user.getId(), from, to, null);
        Map<String, BigDecimal> expenseBreakdown = new HashMap<>();
        for (ExpenseEntry entry : expenseEntries) {
            String category = entry.getCategory();
            expenseBreakdown.put(category, expenseBreakdown.getOrDefault(category, BigDecimal.ZERO).add(entry.getAmount()));
        }

        List<Map<String, Object>> breakdown = expenseBreakdown.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("category", entry.getKey());
                item.put("amount", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());

        Map<String, Object> salaryMap = new HashMap<>();
        salaryMap.put("gross", grossSalary);
        salaryMap.put("net", netSalary);

        Map<String, Object> response = new HashMap<>();
        response.put("month", month.toString());
        response.put("salary", salaryMap);
        response.put("expenses", totalExpenses);
        response.put("investments", totalInvested);
        response.put("debtPayments", totalDebtPaid);
        response.put("netChange", netChange);
        response.put("expenseBreakdown", breakdown);
        ctx.json(response);
    }

    public void yearly(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }
        String yearStr = ctx.queryParam("year");
        int year = yearStr != null && !yearStr.isBlank() ? Integer.parseInt(yearStr) : LocalDate.now().getYear();

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<SalaryEntry> salaries = salaryDao.findByUserAndDateRange(user.getId(), yearStart, yearEnd);
        BigDecimal totalSalary = salaries.stream()
            .map(SalaryEntry::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = expenseDao.sumByUserAndDateRange(user.getId(), yearStart, yearEnd);

        List<Investment> investments = investmentDao.findByUserAndFilters(user.getId(), yearStart, yearEnd, null);
        BigDecimal totalInvestments = investments.stream()
            .map(Investment::getInvestedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal savings = totalSalary.subtract(totalExpenses).subtract(totalInvestments);
        BigDecimal savingsRate = totalSalary.compareTo(BigDecimal.ZERO) > 0
            ? savings.divide(totalSalary, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            LocalDate monthFrom = ym.atDay(1);
            LocalDate monthTo = ym.atEndOfMonth();

            List<SalaryEntry> monthSalaries = salaryDao.findByUserAndDateRange(user.getId(), monthFrom, monthTo);
            BigDecimal monthSalary = monthSalaries.stream()
                .map(SalaryEntry::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthExpenses = expenseDao.sumByUserAndDateRange(user.getId(), monthFrom, monthTo);

            List<Investment> monthInvestments = investmentDao.findByUserAndFilters(user.getId(), monthFrom, monthTo, null);
            BigDecimal monthInvested = monthInvestments.stream()
                .map(Investment::getInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal monthSavings = monthSalary.subtract(monthExpenses).subtract(monthInvested);

            Map<String, Object> trendEntry = new HashMap<>();
            trendEntry.put("month", months[m - 1]);
            trendEntry.put("salary", monthSalary);
            trendEntry.put("expenses", monthExpenses);
            trendEntry.put("savings", monthSavings);
            monthlyTrend.add(trendEntry);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("year", year);
        response.put("totalSalary", totalSalary);
        response.put("totalExpenses", totalExpenses);
        response.put("totalInvestments", totalInvestments);
        response.put("savingsRate", savingsRate);
        response.put("monthlyTrend", monthlyTrend);
        ctx.json(response);
    }

    public void networth(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(401).json(Map.of("error", "Unauthorized"));
            return;
        }

        List<Investment> investments = investmentDao.findByUserAndFilters(user.getId(), null, null, null);
        BigDecimal investmentValue = investments.stream()
            .map(Investment::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get all debts (pass null status to get all)
        List<DebtEntry> debts = debtDao.findByUserAndStatus(user.getId(), null);
        BigDecimal totalDebt = debts.stream()
            .map(DebtEntry::getRemainingBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netWorth = investmentValue.subtract(totalDebt);

        Map<String, BigDecimal> investmentByType = new HashMap<>();
        for (Investment inv : investments) {
            String type = inv.getType();
            investmentByType.put(type, investmentByType.getOrDefault(type, BigDecimal.ZERO).add(inv.getCurrentValue()));
        }

        List<Map<String, Object>> investmentBreakdown = investmentByType.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("type", entry.getKey());
                item.put("value", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("investmentValue", investmentValue);
        response.put("totalDebt", totalDebt);
        response.put("netWorth", netWorth);
        response.put("investmentBreakdown", investmentBreakdown);
        ctx.json(response);
    }
}
