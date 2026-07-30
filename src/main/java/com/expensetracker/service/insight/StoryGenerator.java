package com.expensetracker.service.insight;

import com.expensetracker.entity.Currency;
import com.expensetracker.entity.CurrencyFormatter;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoryGenerator implements InsightGenerator {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MMMM yyyy");

    @Override
    public String type() { return "STORY"; }

    @Override
    public InsightResult generate(Long userId, YearMonth month) {
        Currency currency = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId))
                .getPreferredCurrency();

        String monthLabel = month.format(MONTH_FORMAT);
        List<String> sentences = new ArrayList<>();

        String top = analyticsService.topCategory(userId, month);
        BigDecimal total = analyticsService.totalSpend(userId, month);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            sentences.add("No expenses recorded in " + monthLabel + ".");
            return new InsightResult("STORY", sentences);
        }

        sentences.add("In " + monthLabel + ", you spent the most on " + top + ".");
        sentences.add("Your total spending was " + CurrencyFormatter.format(currency, total) + ".");

        double pct = analyticsService.percentChangeVsLastMonth(userId, month);
        if (pct == 0.0) {
            sentences.add("No previous month data to compare — this may be your first recorded month.");
        } else if (pct > 0) {
            sentences.add(String.format(
                    "Overall spending increased by %.1f%% compared to last month.", pct));
        } else {
            sentences.add(String.format(
                    "Overall spending decreased by %.1f%% compared to last month.", Math.abs(pct)));
        }

        Map<String, BigDecimal> byCategory = analyticsService.sumByCategory(userId, month);
        if (byCategory.size() > 1) {
            long count = byCategory.size();
            sentences.add("Spending was spread across " + count + " categories: "
                    + String.join(", ", byCategory.keySet()) + ".");
        }

        return new InsightResult("STORY", sentences);
    }
}
