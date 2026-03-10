package expensetracker.util;

import expensetracker.model.DebtEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DebtCalculator {

    private static final int MAX_MONTHS = 360;
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public static BigDecimal monthlyInterest(BigDecimal balance, BigDecimal annualRate) {
        if (balance == null || annualRate == null) {
            return BigDecimal.ZERO;
        }
        return balance.multiply(annualRate)
                .divide(HUNDRED, 10, RoundingMode.HALF_UP)
                .divide(TWELVE, 10, RoundingMode.HALF_UP);
    }

    public static int estimateMonthsToPayoff(BigDecimal balance, BigDecimal annualRate, BigDecimal monthlyPayment) {
        if (balance == null || annualRate == null || monthlyPayment == null) {
            return 0;
        }
        if (monthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return MAX_MONTHS;
        }

        BigDecimal currentBalance = balance;
        int months = 0;

        while (currentBalance.compareTo(BigDecimal.ZERO) > 0 && months < MAX_MONTHS) {
            BigDecimal interest = monthlyInterest(currentBalance, annualRate);
            BigDecimal principalPayment = monthlyPayment.subtract(interest);

            if (principalPayment.compareTo(BigDecimal.ZERO) <= 0) {
                return MAX_MONTHS;
            }

            currentBalance = currentBalance.subtract(principalPayment);
            months++;
        }

        return months;
    }

    public static List<Map<String, Object>> calculatePayoffSchedule(
            List<DebtEntry> debts, BigDecimal totalMonthlyPayment, String strategy) {

        if (debts == null || debts.isEmpty() || totalMonthlyPayment == null ||
            totalMonthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
            return new ArrayList<>();
        }

        List<DebtEntry> sortedDebts = sortDebts(debts, strategy);
        List<Map<String, Object>> schedule = new ArrayList<>();
        Map<String, BigDecimal> debtBalances = new HashMap<>();
        Map<String, BigDecimal> debtRates = new HashMap<>();
        Map<String, BigDecimal> debtEmis = new HashMap<>();

        for (DebtEntry d : sortedDebts) {
            debtBalances.put(d.getId(), d.getRemainingBalance());
            debtRates.put(d.getId(), d.getInterestRate());
            debtEmis.put(d.getId(), d.getEmiAmount());
        }

        for (int month = 1; month <= MAX_MONTHS; month++) {
            if (debtBalances.values().stream().allMatch(b -> b.compareTo(BigDecimal.ZERO) <= 0)) {
                break;
            }

            BigDecimal remainingPayment = totalMonthlyPayment;
            List<String> activeDebts = debtBalances.entrySet().stream()
                    .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            for (String debtId : activeDebts) {
                BigDecimal balance = debtBalances.get(debtId);
                BigDecimal rate = debtRates.get(debtId);
                BigDecimal emi = debtEmis.get(debtId);

                BigDecimal interest = monthlyInterest(balance, rate);
                BigDecimal minimumPayment = interest.add(emi);

                if (minimumPayment.compareTo(remainingPayment) > 0) {
                    minimumPayment = remainingPayment;
                }

                BigDecimal principalPayment = minimumPayment.subtract(interest);
                BigDecimal newBalance = balance.subtract(principalPayment);

                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    newBalance = BigDecimal.ZERO;
                }

                debtBalances.put(debtId, newBalance);
                remainingPayment = remainingPayment.subtract(minimumPayment);

                if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }

            for (String debtId : activeDebts) {
                BigDecimal balance = debtBalances.get(debtId);
                BigDecimal rate = debtRates.get(debtId);

                BigDecimal interest = monthlyInterest(balance, rate);
                BigDecimal emi = debtEmis.get(debtId);
                BigDecimal minimumPayment = interest.add(emi);

                if (minimumPayment.compareTo(totalMonthlyPayment) > 0) {
                    minimumPayment = totalMonthlyPayment;
                }

                BigDecimal principalPayment = minimumPayment.subtract(interest);

                Map<String, Object> entry = new HashMap<>();
                entry.put("month", month);
                entry.put("debtId", debtId);
                entry.put("debtName", findDebtName(debts, debtId));
                entry.put("payment", minimumPayment);
                entry.put("interestPaid", interest);
                entry.put("principalPaid", principalPayment);
                entry.put("remainingBalance", debtBalances.get(debtId));

                schedule.add(entry);
            }
        }

        return schedule;
    }

    private static List<DebtEntry> sortDebts(List<DebtEntry> debts, String strategy) {
        if ("AVALANCHE".equalsIgnoreCase(strategy)) {
            return debts.stream()
                    .sorted((d1, d2) -> d2.getInterestRate().compareTo(d1.getInterestRate()))
                    .collect(Collectors.toList());
        } else {
            return debts.stream()
                    .sorted((d1, d2) -> d1.getRemainingBalance().compareTo(d2.getRemainingBalance()))
                    .collect(Collectors.toList());
        }
    }

    private static String findDebtName(List<DebtEntry> debts, String debtId) {
        return debts.stream()
                .filter(d -> d.getId().equals(debtId))
                .map(DebtEntry::getName)
                .findFirst()
                .orElse("");
    }
}
