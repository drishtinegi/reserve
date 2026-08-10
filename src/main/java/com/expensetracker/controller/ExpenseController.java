package com.expensetracker.controller;

import com.expensetracker.dto.ExpenseDtos.ExpenseRequest;
import com.expensetracker.dto.ExpenseDtos.ExpenseResponse;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenses")
@Tag(name = "Expenses", description = "Create, read, update, and delete expenses for the logged-in user")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserRepository userRepository;

    public ExpenseController(ExpenseService expenseService, UserRepository userRepository) {
        this.expenseService = expenseService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Create a new expense — include amount, category, date, mood, and currency")
    @PostMapping
    public ResponseEntity<ExpenseResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ExpenseRequest req) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.create(userId, req));
    }

    @Operation(summary = "List expenses for the logged-in user — paginated (page=0, size=20 by default)")
    @GetMapping
    public ResponseEntity<Page<ExpenseResponse>> listAll(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = resolveUserId(userDetails);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("date").descending());
        return ResponseEntity.ok(expenseService.listPaged(userId, pageable));
    }

    @Operation(summary = "Get a single expense by ID — must belong to the logged-in user")
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(expenseService.getById(userId, id));
    }

    @Operation(summary = "Update an existing expense — replaces all fields")
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest req) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(expenseService.update(userId, id, req));
    }

    @Operation(summary = "Delete an expense by ID — must belong to the logged-in user")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = resolveUserId(userDetails);
        expenseService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."))
                .getId();
    }
}
