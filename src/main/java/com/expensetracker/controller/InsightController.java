package com.expensetracker.controller;

import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.insight.InsightGenerator;
import com.expensetracker.service.insight.InsightResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/insights")
@Tag(name = "Insights", description = "AI-style spending insights: story, mood, prediction, gamification")
public class InsightController {

    private final List<InsightGenerator> generators;
    private final UserRepository userRepository;

    public InsightController(List<InsightGenerator> generators, UserRepository userRepository) {
        this.generators = generators;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Monthly narrative recap — top category, total spend, month-over-month change")
    @GetMapping("/story")
    public ResponseEntity<InsightResult> story(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String month) {
        return ResponseEntity.ok(
                findGenerator("STORY").generate(resolveUserId(userDetails), YearMonth.parse(month)));
    }

    @Operation(summary = "Mood analysis — which emotional state drives the most spending")
    @GetMapping("/mood")
    public ResponseEntity<InsightResult> mood(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String month) {
        return ResponseEntity.ok(
                findGenerator("MOOD").generate(resolveUserId(userDetails), YearMonth.parse(month)));
    }

    @Operation(summary = "Spending forecast — projected 6-month total and what-if savings scenario")
    @GetMapping("/prediction")
    public ResponseEntity<InsightResult> prediction(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String month) {
        return ResponseEntity.ok(
                findGenerator("PREDICTION").generate(resolveUserId(userDetails), YearMonth.parse(month)));
    }

    @Operation(summary = "Budget streaks — how many consecutive months you've stayed under budget per category")
    @GetMapping("/gamification")
    public ResponseEntity<InsightResult> gamification(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String month) {
        return ResponseEntity.ok(
                findGenerator("GAMIFICATION").generate(resolveUserId(userDetails), YearMonth.parse(month)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private InsightGenerator findGenerator(String type) {
        return generators.stream()
                .filter(g -> g.type().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No generator found for type: " + type));
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."))
                .getId();
    }
}
