package com.expensetracker.service.insight;

import com.expensetracker.entity.Currency;
import com.expensetracker.entity.CurrencyFormatter;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.AnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class PredictionGenerator implements InsightGenerator {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    public PredictionGenerator(AnalyticsService analyticsService, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    private static final int MONTHS_BACK = 6;
    private static final int MONTHS_FORWARD = 6;

    @Override
    public String type() { return "PREDICTION"; }

    @Override
    public InsightResult generate(Long userId, YearMonth month) {
        Currency currency = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId))
                .getPreferredCurrency();

        List<String> sentences = new ArrayList<>();
        List<BigDecimal> trend = analyticsService.monthlyTotalsTrend(userId, MONTHS_BACK);

        long nonZeroMonths = trend.stream()
                .filter(t -> t.compareTo(BigDecimal.ZERO) > 0)
                .count();

        if (nonZeroMonths < 2) {
            sentences.add("Not enough history yet for a reliable forecast — "
                    + "add a few more months of expenses to unlock predictions.");
            return new InsightResult("PREDICTION", sentences);
        }

        BigDecimal sum = trend.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgMonthly = sum.divide(
                BigDecimal.valueOf(MONTHS_BACK), 2, RoundingMode.HALF_UP);

        BigDecimal projectedSixMonths = avgMonthly.multiply(BigDecimal.valueOf(MONTHS_FORWARD))
                .setScale(2, RoundingMode.HALF_UP);

        sentences.add(String.format(
                "At your current pace, projected spend over the next %d months is approximately %s.",
                MONTHS_FORWARD, CurrencyFormatter.format(currency, projectedSixMonths)));

        String topCat = analyticsService.topCategory(userId, month);

        if (!"N/A".equals(topCat)) {
            List<BigDecimal> reducedTrend = new ArrayList<>();
            YearMonth current = YearMonth.now();
            for (int i = MONTHS_BACK - 1; i >= 0; i--) {
                YearMonth m = current.minusMonths(i);
                BigDecimal monthTotal = analyticsService.totalSpend(userId, m);
                BigDecimal catTotal = analyticsService.sumByCategory(userId, m)
                        .getOrDefault(topCat, BigDecimal.ZERO);
                reducedTrend.add(monthTotal.subtract(catTotal).max(BigDecimal.ZERO));
            }

            BigDecimal reducedSum = reducedTrend.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal reducedAvg = reducedSum.divide(
                    BigDecimal.valueOf(MONTHS_BACK), 2, RoundingMode.HALF_UP);

            BigDecimal yearlySaving = avgMonthly.subtract(reducedAvg)
                    .multiply(BigDecimal.valueOf(12))
                    .setScale(2, RoundingMode.HALF_UP);

            if (yearlySaving.compareTo(BigDecimal.ZERO) > 0) {
                sentences.add(String.format(
                        "If you stopped spending on %s entirely, you could save approximately %s per year.",
                        topCat, CurrencyFormatter.format(currency, yearlySaving)));
            }
        }

        return new InsightResult("PREDICTION", sentences);
    }
}
