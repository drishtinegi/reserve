package com.expensetracker.service;

import com.expensetracker.dto.ExpenseDtos.ExpenseRequest;
import com.expensetracker.dto.ExpenseDtos.ExpenseResponse;
import com.expensetracker.entity.Expense;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.ExpenseRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ExpenseService — contains all business logic for expenses.
 * Controllers call this; this calls the repository.
 * Controllers never touch the repository directly.
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ExpenseResponse create(Long userId, ExpenseRequest req) {
        // Always derive currency from the user's account — never trust the request body
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setCurrency(user.getPreferredCurrency());
        applyRequest(expense, req);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    public List<ExpenseResponse> listAll(Long userId) {
        return expenseRepository.findByUserId(userId)
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    public Page<ExpenseResponse> listPaged(Long userId, Pageable pageable) {
        return expenseRepository.findByUserId(userId, pageable)
                .map(ExpenseResponse::from);
    }

    public ExpenseResponse getById(Long userId, Long expenseId) {
        Expense expense = findOwned(userId, expenseId);
        return ExpenseResponse.from(expense);
    }

    public ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest req) {
        Expense expense = findOwned(userId, expenseId);
        applyRequest(expense, req);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    public void delete(Long userId, Long expenseId) {
        Expense expense = findOwned(userId, expenseId);
        expenseRepository.delete(expense);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Fetches an expense by id and verifies it belongs to the given user.
     * Throws 404 if not found, 403 if owned by someone else.
     */
    private Expense findOwned(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expense not found: " + expenseId));
        if (!expense.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Expense " + expenseId + " does not belong to you.");
        }
        return expense;
    }

    /** Copies safe fields from an incoming request onto an entity.
     *  Currency is intentionally NOT set here — it is always derived from the user's account. */
    private void applyRequest(Expense expense, ExpenseRequest req) {
        expense.setAmount(req.amount());
        expense.setCategory(req.category());
        expense.setDescription(req.description());
        expense.setDate(req.date());
        expense.setMood(req.mood());
        expense.setPaymentMethod(req.paymentMethod());
    }
}
