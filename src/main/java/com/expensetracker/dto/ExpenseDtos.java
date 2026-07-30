package com.expensetracker.dto;

import com.expensetracker.entity.Currency;
import com.expensetracker.entity.Expense;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExpenseDtos {

    // ── Incoming: what the client sends to create or update an expense ───────
    // Note: `currency` is intentionally absent. The server always uses the
    // logged-in user's preferredCurrency, never a client-supplied value.
    public record ExpenseRequest(
            @NotNull @Positive BigDecimal amount,
            @NotBlank String category,
            String description,
            @NotNull LocalDate date,
            @NotNull Expense.Mood mood,
            String paymentMethod
    ) {}

    // ── Outgoing: what we send back — safe, shaped data ─────────────────────
    public record ExpenseResponse(
            Long id,
            Long userId,
            BigDecimal amount,
            String category,
            String description,
            LocalDate date,
            Expense.Mood mood,
            Currency currency,
            String paymentMethod,
            LocalDateTime createdAt
    ) {
        // Static factory: converts an Expense entity into this response record.
        // Keeps conversion logic here so controllers and services don't repeat it.
        public static ExpenseResponse from(Expense e) {
            return new ExpenseResponse(
                    e.getId(),
                    e.getUserId(),
                    e.getAmount(),
                    e.getCategory(),
                    e.getDescription(),
                    e.getDate(),
                    e.getMood(),
                    e.getCurrency(),
                    e.getPaymentMethod(),
                    e.getCreatedAt()
            );
        }
    }
}
