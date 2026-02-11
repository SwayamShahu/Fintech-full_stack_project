package com.fintrack.controller;

import com.fintrack.dto.ApiResponse;
import com.fintrack.dto.RecurringExpenseRequest;
import com.fintrack.dto.RecurringExpenseResponse;
import com.fintrack.security.CustomUserDetails;
import com.fintrack.service.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    @PostMapping
    public ResponseEntity<ApiResponse> createRecurringExpense(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RecurringExpenseRequest request) {
        try {
            RecurringExpenseResponse response = recurringExpenseService
                    .createRecurringExpense(userDetails.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Recurring expense created", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllRecurringExpenses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RecurringExpenseResponse> expenses = recurringExpenseService
                .getAllRecurringExpenses(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Recurring expenses retrieved", expenses));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse> getActiveRecurringExpenses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RecurringExpenseResponse> expenses = recurringExpenseService
                .getActiveRecurringExpenses(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Active recurring expenses retrieved", expenses));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse> getUpcomingDueExpenses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "7") int days) {
        List<RecurringExpenseResponse> expenses = recurringExpenseService
                .getUpcomingDueExpenses(userDetails.getId(), days);
        return ResponseEntity.ok(new ApiResponse(true, "Upcoming recurring expenses retrieved", expenses));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateRecurringExpense(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody RecurringExpenseRequest request) {
        try {
            RecurringExpenseResponse response = recurringExpenseService
                    .updateRecurringExpense(userDetails.getId(), id, request);
            return ResponseEntity.ok(new ApiResponse(true, "Recurring expense updated", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse> deactivateRecurringExpense(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        try {
            recurringExpenseService.deactivateRecurringExpense(userDetails.getId(), id);
            return ResponseEntity.ok(new ApiResponse(true, "Recurring expense deactivated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRecurringExpense(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        try {
            recurringExpenseService.deleteRecurringExpense(userDetails.getId(), id);
            return ResponseEntity.ok(new ApiResponse(true, "Recurring expense deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/process-now")
    public ResponseEntity<ApiResponse> processRecurringExpensesNow() {
        try {
            recurringExpenseService.processRecurringExpenses();
            return ResponseEntity.ok(new ApiResponse(true, "Recurring expenses processed"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
}
