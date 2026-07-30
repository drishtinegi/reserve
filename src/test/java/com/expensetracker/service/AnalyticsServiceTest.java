package com.expensetracker.service;

import com.expensetracker.entity.Budget;
import com.expensetracker.entity.Currency;
import com.expensetracker.entity.Expense;
import com.expensetracker.entity.Expense.Mood;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnalyticsService.
 *
 * These tests do NOT start a Spring context or hit a real database.
 * Mockito replaces the repositories with fake objects whose return values
 * we control — so each test is fast and focused on one behaviour at a time.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private static final Long USER_ID = 1L;
    private static final YearMonth JULY = YearMonth.of(2026, 7);

    // ── Helper: build a minimal Expense object for use in tests ──────────────
    private Expense expense(String category, double amount, Mood mood) {
        Expense e = new Expense();
        e.setUserId(USER_ID);
        e.setCategory(category);
        e.setAmount(BigDecimal.valueOf(amount));
        e.setMood(mood);
        e.setCurrency(Currency.USD);
        e.setDate(LocalDate.of(2026, 7, 10));
        return e;
    }

    @BeforeEach
    void stubExpensesForJuly() {
        // Shared stub: three expenses in July 2026 used by most tests
        List<Expense> july = List.of(
                expense("Food",      120.00, Mood.HAPPY),
                expense("Food",       80.00, Mood.STRESSED),
                expense("Transport",  45.00, Mood.NEUTRAL)
        );
        when(expenseRepository.findByUserIdAndDateBetween(
                eq(USER_ID), eq(JULY.atDay(1)), eq(JULY.atEndOfMonth())))
                .thenReturn(july);
    }

    // ── Test 1: sumByCategory groups and totals correctly ────────────────────
    @Test
    void sumByCategory_groupsAndSumsCorrectly() {
        Map<String, BigDecimal> result = analyticsService.sumByCategory(USER_ID, JULY);

        assertThat(result).containsKey("Food");
        assertThat(result).containsKey("Transport");
        assertThat(result.get("Food")).isEqualByComparingTo("200.00");
        assertThat(result.get("Transport")).isEqualByComparingTo("45.00");
    }

    // ── Test 2: topCategory returns the category with the highest total ───────
    @Test
    void topCategory_returnsHighestSpendCategory() {
        String top = analyticsService.topCategory(USER_ID, JULY);
        assertThat(top).isEqualTo("Food");
    }

    // ── Test 3: percentChange returns 0 when there is no last-month data ─────
    @Test
    void percentChangeVsLastMonth_returnsZeroWhenNoPreviousData() {
        YearMonth june = JULY.minusMonths(1);

        // Stub June to return an empty list (no expenses last month)
        when(expenseRepository.findByUserIdAndDateBetween(
                eq(USER_ID), eq(june.atDay(1)), eq(june.atEndOfMonth())))
                .thenReturn(List.of());

        double change = analyticsService.percentChangeVsLastMonth(USER_ID, JULY);
        assertThat(change).isEqualTo(0.0);
    }

    // ── Test 4: percentChange calculates correctly when previous data exists ──
    @Test
    void percentChangeVsLastMonth_calculatesCorrectly() {
        YearMonth june = JULY.minusMonths(1);

        // June had one expense: $200
        Expense juneExpense = expense("Food", 200.00, Mood.NEUTRAL);
        juneExpense.setDate(LocalDate.of(2026, 6, 15));
        when(expenseRepository.findByUserIdAndDateBetween(
                eq(USER_ID), eq(june.atDay(1)), eq(june.atEndOfMonth())))
                .thenReturn(List.of(juneExpense));

        // July total = 120 + 80 + 45 = 245; change = (245-200)/200*100 = 22.5%
        double change = analyticsService.percentChangeVsLastMonth(USER_ID, JULY);
        assertThat(change).isCloseTo(22.5, within(0.1));
    }

    // ── Test 5: isUnderBudget returns false when no budget is set ────────────
    @Test
    void isUnderBudget_returnsFalseWhenNoBudgetExists() {
        when(budgetRepository.findByUserIdAndCategory(USER_ID, "Food"))
                .thenReturn(Optional.empty());

        boolean result = analyticsService.isUnderBudget(USER_ID, "Food", JULY);
        assertThat(result).isFalse();
    }

    // ── Test 6: isUnderBudget returns true when spending is below the limit ───
    @Test
    void isUnderBudget_returnsTrueWhenUnderLimit() {
        Budget budget = new Budget();
        budget.setMonthlyLimit(BigDecimal.valueOf(300.00)); // Food spent = 200, limit = 300
        when(budgetRepository.findByUserIdAndCategory(USER_ID, "Food"))
                .thenReturn(Optional.of(budget));

        boolean result = analyticsService.isUnderBudget(USER_ID, "Food", JULY);
        assertThat(result).isTrue();
    }
}
