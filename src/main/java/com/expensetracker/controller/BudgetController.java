package com.expensetracker.controller;

import com.expensetracker.entity.Budget;
import com.expensetracker.entity.User;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/budgets")
@Tag(name = "Budgets", description = "Create and view monthly category budgets")
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public BudgetController(BudgetRepository budgetRepository, UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
    }

    // Currency is intentionally absent from this request — it is always
    // derived from the logged-in user's preferredCurrency, same as expenses.
    record BudgetRequest(
            @NotBlank String category,
            @NotNull @Positive BigDecimal monthlyLimit
    ) {}

    @Operation(summary = "Create or update a monthly budget for a category")
    @PostMapping
    public ResponseEntity<Budget> createOrUpdate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BudgetRequest req) {

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Budget budget = budgetRepository
                .findByUserIdAndCategory(user.getId(), req.category())
                .orElse(new Budget());

        budget.setUserId(user.getId());
        budget.setCategory(req.category());
        budget.setMonthlyLimit(req.monthlyLimit());
        budget.setCurrency(user.getPreferredCurrency()); // always from account, never from request

        return ResponseEntity.status(HttpStatus.CREATED).body(budgetRepository.save(budget));
    }

    @Operation(summary = "List all budgets for the logged-in user")
    @GetMapping
    public ResponseEntity<List<Budget>> listAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."))
                .getId();
        return ResponseEntity.ok(budgetRepository.findByUserId(userId));
    }
}
