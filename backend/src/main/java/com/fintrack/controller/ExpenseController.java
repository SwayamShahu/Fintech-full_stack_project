package com.fintrack.controller;

import com.fintrack.dto.ApiResponse;
import com.fintrack.dto.ExpenseRequest;
import com.fintrack.dto.ExpenseResponse;
import com.fintrack.security.CustomUserDetails;
import com.fintrack.service.ExpenseService;
import com.fintrack.service.MLAnomalyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final MLAnomalyService mlAnomalyService;

    @PostMapping
    public ResponseEntity<ApiResponse> createExpense(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExpenseRequest request) {
        try {
            ExpenseResponse expense = expenseService.createExpense(userDetails.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Expense created successfully", expense));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllExpenses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ExpenseResponse> expenses = expenseService.getAllExpenses(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Expenses retrieved", expenses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getExpenseById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        try {
            ExpenseResponse expense = expenseService.getExpenseById(userDetails.getId(), id);
            return ResponseEntity.ok(new ApiResponse(true, "Expense retrieved", expense));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/range")
    public ResponseEntity<ApiResponse> getExpensesByDateRange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByDateRange(
                userDetails.getId(), startDate, endDate);
        return ResponseEntity.ok(new ApiResponse(true, "Expenses retrieved", expenses));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse> getExpensesByCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId) {
        List<ExpenseResponse> expenses = expenseService.getExpensesByCategory(
                userDetails.getId(), categoryId);
        return ResponseEntity.ok(new ApiResponse(true, "Expenses retrieved", expenses));
    }

    @GetMapping("/anomalies")
    public ResponseEntity<ApiResponse> getAnomalies(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ExpenseResponse> anomalies = expenseService.getAnomalies(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Anomalies retrieved", anomalies));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateExpense(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        try {
            ExpenseResponse expense = expenseService.updateExpense(userDetails.getId(), id, request);
            return ResponseEntity.ok(new ApiResponse(true, "Expense updated successfully", expense));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteExpense(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        try {
            expenseService.deleteExpense(userDetails.getId(), id);
            return ResponseEntity.ok(new ApiResponse(true, "Expense deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/ml-status")
    public ResponseEntity<ApiResponse> getMLServiceStatus() {
        Map<String, Object> status = mlAnomalyService.getServiceStatus();
        boolean healthy = mlAnomalyService.isServiceHealthy();
        status.put("healthy", healthy);
        return ResponseEntity.ok(new ApiResponse(true, "ML service status", status));
    }
}
