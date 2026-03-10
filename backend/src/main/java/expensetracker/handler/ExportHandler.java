package expensetracker.handler;

import expensetracker.dao.*;
import expensetracker.model.*;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ExportHandler {

    private final ExpenseDao expenseDao = new ExpenseDao();
    private final InvestmentDao investmentDao = new InvestmentDao();
    private final SalaryDao salaryDao = new SalaryDao();
    private final DebtDao debtDao = new DebtDao();

    /** GET /api/export/expenses?format=csv – export expenses as CSV. */
    public void exportExpenses(Context ctx) {
        try {
            User user = ctx.attribute("user");
            String startDate = ctx.queryParam("startDate");
            String endDate = ctx.queryParam("endDate");
            String category = ctx.queryParam("category");

            LocalDate from = startDate != null ? LocalDate.parse(startDate) : null;
            LocalDate to = endDate != null ? LocalDate.parse(endDate) : null;

            List<ExpenseEntry> expenses = expenseDao.findByUserAndFilters(user.getId(), from, to, category);

            StringBuilder csv = new StringBuilder();
            csv.append("Date,Category,Amount,Currency,Loan Name,Note\n");
            for (ExpenseEntry e : expenses) {
                csv.append(escapeCsv(String.valueOf(e.getEntryDate()))).append(",");
                csv.append(escapeCsv(e.getCategory())).append(",");
                csv.append(e.getAmount()).append(",");
                csv.append(escapeCsv(e.getCurrency())).append(",");
                csv.append(escapeCsv(e.getLoanName() != null ? e.getLoanName() : "")).append(",");
                csv.append(escapeCsv(e.getNote() != null ? e.getNote() : "")).append("\n");
            }

            ctx.contentType("text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"expenses.csv\"");
            ctx.result(csv.toString());
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Failed to export expenses"));
        }
    }

    /** GET /api/export/investments?format=csv – export investments as CSV. */
    public void exportInvestments(Context ctx) {
        try {
            User user = ctx.attribute("user");
            List<Investment> investments = investmentDao.findByUserAndFilters(user.getId(), null, null, null);

            StringBuilder csv = new StringBuilder();
            csv.append("Name,Type,Invested Amount,Current Value,Units,NAV,Date,Notes\n");
            for (Investment inv : investments) {
                csv.append(escapeCsv(inv.getName())).append(",");
                csv.append(escapeCsv(inv.getType())).append(",");
                csv.append(inv.getInvestedAmount()).append(",");
                csv.append(inv.getCurrentValue()).append(",");
                csv.append(inv.getUnits() != null ? inv.getUnits() : "").append(",");
                csv.append(inv.getNavPrice() != null ? inv.getNavPrice() : "").append(",");
                csv.append(escapeCsv(String.valueOf(inv.getEntryDate()))).append(",");
                csv.append(escapeCsv(inv.getNotes() != null ? inv.getNotes() : "")).append("\n");
            }

            ctx.contentType("text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"investments.csv\"");
            ctx.result(csv.toString());
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Failed to export investments"));
        }
    }

    /** GET /api/export/salary – export salary entries as CSV. */
    public void exportSalary(Context ctx) {
        try {
            User user = ctx.attribute("user");
            List<SalaryEntry> entries = salaryDao.findByUserAndDateRange(user.getId(), null, null);

            StringBuilder csv = new StringBuilder();
            csv.append("Month,Gross Amount,Deductions,Net Amount,Notes\n");
            for (SalaryEntry s : entries) {
                csv.append(escapeCsv(String.valueOf(s.getMonth()))).append(",");
                csv.append(s.getGrossAmount()).append(",");
                csv.append(s.getDeductions()).append(",");
                csv.append(s.getNetAmount()).append(",");
                csv.append(escapeCsv(s.getNotes() != null ? s.getNotes() : "")).append("\n");
            }

            ctx.contentType("text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"salary.csv\"");
            ctx.result(csv.toString());
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Failed to export salary data"));
        }
    }

    /** GET /api/export/debts – export debts as CSV. */
    public void exportDebts(Context ctx) {
        try {
            User user = ctx.attribute("user");
            List<DebtEntry> debts = debtDao.findByUserAndStatus(user.getId(), null);

            StringBuilder csv = new StringBuilder();
            csv.append("Name,Type,Principal,Interest Rate,EMI,Remaining Balance,Status,Start Date,End Date\n");
            for (DebtEntry d : debts) {
                csv.append(escapeCsv(d.getName())).append(",");
                csv.append(escapeCsv(d.getType())).append(",");
                csv.append(d.getPrincipalAmount()).append(",");
                csv.append(d.getInterestRate()).append(",");
                csv.append(d.getEmiAmount()).append(",");
                csv.append(d.getRemainingBalance()).append(",");
                csv.append(escapeCsv(d.getStatus())).append(",");
                csv.append(escapeCsv(String.valueOf(d.getStartDate()))).append(",");
                csv.append(escapeCsv(d.getEndDate() != null ? String.valueOf(d.getEndDate()) : "")).append("\n");
            }

            ctx.contentType("text/csv");
            ctx.header("Content-Disposition", "attachment; filename=\"debts.csv\"");
            ctx.result(csv.toString());
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", "Failed to export debts"));
        }
    }

    /** Escape a field for CSV (wrap in quotes if it contains comma, quote, or newline). */
    private String escapeCsv(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
