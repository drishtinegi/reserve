package com.expensetracker.service.insight;

import com.expensetracker.entity.Budget;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * GamificationGenerator — detects and reports budget-under streaks.
 *
 * For each category the user has a budget for, it walks backward month by
 * month (up to a reasonable limit) to find the longest consecutive streak
 * of months where spending stayed under the budget limit.
 *
 * All "did I spend under budget?" checks go through AnalyticsService.isUnderBudget().
 * This class never touches ExpenseRepository or BudgetRepository for totals.
 */
@Service
@RequiredArgsConstructor
public class GamificationGenerator implements InsightGenerator {

    private final AnalyticsService analyticsService;
    private final BudgetRepository budgetRepository;

    // How far back to look when counting a streak
    private static final int MAX_STREAK_LOOKBACK = 12;

    @Override
    public String type() { return "GAMIFICATION"; }

    @Override
    public InsightResult generate(Long userId, YearMonth month) {
        List<String> sentences = new ArrayList<>();

        List<Budget> budgets = budgetRepository.findByUserId(userId);

        // ── Graceful fallback: user hasn't set any budgets yet ────────────────
        if (budgets.isEmpty()) {
            sentences.add("Set a budget to start tracking your streaks.");
            return new InsightResult("GAMIFICATION", sentences);
        }

        int totalStreaks = 0;

        for (Budget budget : budgets) {
            String category = budget.getCategory();
            int streak = computeStreak(userId, category, month);

            if (streak == 0) {
                sentences.add("You went over budget on " + category
                        + " this month — try to get back on track!");
            } else if (streak == 1) {
                sentences.add("You stayed under budget on " + category + " this month. Keep it up!");
            } else {
                sentences.add("You've stayed under budget on " + category
                        + " for " + streak + " months in a row!");
            }
            totalStreaks += streak;
        }

        // ── Summary line if the user is managing multiple categories well ─────
        if (budgets.size() > 1 && totalStreaks == budgets.size()) {
            sentences.add("Great work — you're under budget across all "
                    + budgets.size() + " tracked categories this month!");
        }

        return new InsightResult("GAMIFICATION", sentences);
    }

    /**
     * Walks backward from `month`, counting consecutive months under budget.
     * Stops as soon as a month is over budget, or after MAX_STREAK_LOOKBACK.
     */
    private int computeStreak(Long userId, String category, YearMonth month) {
        int streak = 0;
        for (int i = 0; i < MAX_STREAK_LOOKBACK; i++) {
            YearMonth checkMonth = month.minusMonths(i);
            if (analyticsService.isUnderBudget(userId, category, checkMonth)) {
                streak++;
            } else {
                break; // streak is broken — stop counting
            }
        }
        return streak;
    }
}
