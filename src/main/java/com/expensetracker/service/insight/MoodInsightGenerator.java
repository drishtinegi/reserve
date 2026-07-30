package com.expensetracker.service.insight;

import com.expensetracker.entity.Currency;
import com.expensetracker.entity.CurrencyFormatter;
import com.expensetracker.entity.Expense.Mood;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MoodInsightGenerator implements InsightGenerator {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    @Override
    public String type() { return "MOOD"; }

    @Override
    public InsightResult generate(Long userId, YearMonth month) {
        Currency currency = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId))
                .getPreferredCurrency();

        Map<Mood, BigDecimal> byMood = analyticsService.sumByMood(userId, month);
        List<String> sentences = new ArrayList<>();

        if (byMood.isEmpty()) {
            sentences.add("No expenses recorded this month, so there is no mood data yet.");
            return new InsightResult("MOOD", sentences);
        }

        if (byMood.size() == 1) {
            Map.Entry<Mood, BigDecimal> only = byMood.entrySet().iterator().next();
            sentences.add("Not enough mood data yet this month to find a pattern — "
                    + "all spending so far was logged as "
                    + formatMood(only.getKey()) + ".");
            return new InsightResult("MOOD", sentences);
        }

        Map.Entry<Mood, BigDecimal> highest = byMood.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .orElseThrow();

        Map.Entry<Mood, BigDecimal> lowest = byMood.entrySet().stream()
                .min(Comparator.comparing(Map.Entry::getValue))
                .orElseThrow();

        sentences.add("You spent the most when feeling "
                + formatMood(highest.getKey())
                + " — " + CurrencyFormatter.format(currency, highest.getValue())
                + " this month.");

        BigDecimal low = lowest.getValue();
        BigDecimal high = highest.getValue();

        if (low.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = high.divide(low, 4, RoundingMode.HALF_UP).doubleValue();
            sentences.add(String.format(
                    "That is %.1fx more than your %s spending (%s).",
                    ratio,
                    formatMood(lowest.getKey()),
                    CurrencyFormatter.format(currency, low)));
        } else {
            sentences.add("Your lowest-spend mood ("
                    + formatMood(lowest.getKey())
                    + ") had " + CurrencyFormatter.format(currency, BigDecimal.ZERO)
                    + " in recorded expenses this month.");
        }

        if (byMood.size() >= 3) {
            StringBuilder sb = new StringBuilder("Moods recorded this month: ");
            byMood.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry<Mood, BigDecimal>::getValue).reversed())
                    .forEach(e -> sb.append(formatMood(e.getKey()))
                            .append(" (").append(CurrencyFormatter.format(currency, e.getValue()))
                            .append("), "));
            sb.setLength(sb.length() - 2);
            sb.append(".");
            sentences.add(sb.toString());
        }

        return new InsightResult("MOOD", sentences);
    }

    private String formatMood(Mood mood) {
        String name = mood.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
