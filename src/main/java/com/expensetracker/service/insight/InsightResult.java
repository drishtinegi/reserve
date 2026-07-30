package com.expensetracker.service.insight;

import java.util.List;

/**
 * InsightResult — the standard shape every insight generator returns.
 *
 * type:     which kind of insight this is, e.g. "STORY" or "MOOD"
 * messages: the list of human-readable sentences that make up the insight
 */
public record InsightResult(String type, List<String> messages) {}
