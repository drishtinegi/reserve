package com.expensetracker.service.insight;

import java.time.YearMonth;

/**
 * InsightGenerator — the shared contract every insight feature must honour.
 *
 * Each implementing class must provide:
 *   type()     → a fixed string label like "STORY" or "MOOD"
 *   generate() → the actual insight sentences for a user and month
 *
 * Spring will automatically find every class in the project that implements
 * this interface and collect them into a List<InsightGenerator> when anything
 * asks for one. We don't have to register them manually anywhere.
 */
public interface InsightGenerator {
    /** A fixed label identifying this generator, e.g. "STORY", "MOOD". */
    String type();
    InsightResult generate(Long userId, YearMonth month);
}
