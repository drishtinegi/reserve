package com.expensetracker.service;

import com.expensetracker.entity.Budget;
import com.expensetracker.entity.Expense;
import com.expensetracker.entity.Expense.Mood;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AnalyticsService — the single source of truth for all number-crunching.
 *
 * Every insight feature (Story, Mood, Prediction, Gamification) calls methods
 * here instead of computing its own totals. This means the grouping and summing
 * logic is written exactly once, tested once, and fixed in one place if a bug
 * is ever found.
 */
@Service
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public AnalyticsService(ExpenseRepository expenseRepository, BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    // ── Core helper: fetch all expenses for a user in a given month ──────────

    public List<Expense> getExpensesForMonth(Long userId, YearMonth month) {
        return expenseRepository.findByUserIdAndDateBetween(
                userId,
                month.atDay(1),
                month.atEndOfMonth()
        );
    }

    // ── Public API used by insight generators ────────────────────────────────

    /**
     * Total spent per category this month.
     * e.g. { "Food": 320.00, "Transport": 85.50 }
     */
    public Map<String, BigDecimal> sumByCategory(Long userId, YearMonth month) {
        return getExpensesForMonth(userId, month).stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
    }

    /**
     * The single category with the highest total spend this month.
     * Returns "N/A" if there are no expenses.
     */
    public String topCategory(Long userId, YearMonth month) {
        return sumByCategory(userId, month).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    /**
     * Sum of all expense amounts this month, regardless of category.
     */
    public BigDecimal totalSpend(Long userId, YearMonth month) {
        return getExpensesForMonth(userId, month).stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * How much spending changed this month vs last month, as a percentage.
     * Returns 0.0 if last month had no spending (avoids divide-by-zero).
     * A positive result means spending went up; negative means it went down.
     */
    public double percentChangeVsLastMonth(Long userId, YearMonth month) {
        BigDecimal thisMonth = totalSpend(userId, month);
        BigDecimal lastMonth = totalSpend(userId, month.minusMonths(1));

        if (lastMonth.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        return thisMonth.subtract(lastMonth)
                .divide(lastMonth, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /**
     * Total spent grouped by mood this month.
     * e.g. { HAPPY: 150.00, STRESSED: 340.00, NEUTRAL: 90.00 }
     */
    public Map<Mood, BigDecimal> sumByMood(Long userId, YearMonth month) {
        return getExpensesForMonth(userId, month).stream()
                .collect(Collectors.groupingBy(
                        Expense::getMood,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
    }

    /**
     * Returns the total spend for each of the last `monthsBack` months,
     * ordered oldest → newest. The last element is the given month itself.
     * Months with zero spending still appear in the list as 0.00.
     */
    public List<BigDecimal> monthlyTotalsTrend(Long userId, int monthsBack) {
        List<BigDecimal> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = monthsBack - 1; i >= 0; i--) {
            trend.add(totalSpend(userId, current.minusMonths(i)));
        }
        return trend;
    }

    /**
     * Returns true if the user's spending in the given category and month
     * is strictly less than the Budget.monthlyLimit they've set for it.
     * Returns false if no budget exists for that category, or if over.
     */
    public boolean isUnderBudget(Long userId, String category, YearMonth month) {
        Optional<Budget> budget = budgetRepository.findByUserIdAndCategory(userId, category);
        if (budget.isEmpty()) {
            return false;
        }
        BigDecimal spent = sumByCategory(userId, month)
                .getOrDefault(category, BigDecimal.ZERO);
        return spent.compareTo(budget.get().getMonthlyLimit()) < 0;
    }
}
