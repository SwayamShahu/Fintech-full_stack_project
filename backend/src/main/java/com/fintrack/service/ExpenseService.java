package com.fintrack.service;

import com.fintrack.dto.ExpenseRequest;
import com.fintrack.dto.ExpenseResponse;
import com.fintrack.model.Category;
import com.fintrack.model.Expense;
import com.fintrack.model.User;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.ExpenseRepository;
import com.fintrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ExpenseResponse createExpense(Long userId, ExpenseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Simple anomaly detection based on average
        BigDecimal average = expenseRepository.getAverageByUserId(userId);
        boolean isAnomaly = false;
        BigDecimal anomalyScore = BigDecimal.ZERO;
        String anomalyExplanation = null;

        if (average != null && average.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal threshold = average.multiply(BigDecimal.valueOf(3));
            if (request.getAmount().compareTo(threshold) > 0) {
                isAnomaly = true;
                anomalyScore = request.getAmount().divide(average, 4, BigDecimal.ROUND_HALF_UP);
                anomalyExplanation = "This expense is " + anomalyScore.setScale(1, BigDecimal.ROUND_HALF_UP) 
                        + "x higher than your average spending";
            }
        }

        Expense expense = Expense.builder()
                .user(user)
                .amount(request.getAmount())
                .category(category)
                .description(request.getDescription())
                .expenseDate(request.getExpenseDate())
                .paymentMode(request.getPaymentMode())
                .isRecurring(request.getIsRecurring())
                .isAnomaly(isAnomaly)
                .anomalyScore(anomalyScore)
                .anomalyExplanation(anomalyExplanation)
                .build();

        expense = expenseRepository.save(expense);
        return new ExpenseResponse(expense);
    }

    public List<ExpenseResponse> getAllExpenses(Long userId) {
        return expenseRepository.findByUserIdOrderByExpenseDateDesc(userId)
                .stream()
                .map(ExpenseResponse::new)
                .collect(Collectors.toList());
    }

    public List<ExpenseResponse> getExpensesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(userId, startDate, endDate)
                .stream()
                .map(ExpenseResponse::new)
                .collect(Collectors.toList());
    }

    public List<ExpenseResponse> getExpensesByCategory(Long userId, Long categoryId) {
        return expenseRepository.findByUserIdAndCategoryIdOrderByExpenseDateDesc(userId, categoryId)
                .stream()
                .map(ExpenseResponse::new)
                .collect(Collectors.toList());
    }

    public ExpenseResponse getExpenseById(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to expense");
        }

        return new ExpenseResponse(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long userId, Long expenseId, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to expense");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        expense.setAmount(request.getAmount());
        expense.setCategory(category);
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaymentMode(request.getPaymentMode());
        expense.setIsRecurring(request.getIsRecurring());

        expense = expenseRepository.save(expense);
        return new ExpenseResponse(expense);
    }

    @Transactional
    public void deleteExpense(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to expense");
        }

        expenseRepository.delete(expense);
    }

    public List<ExpenseResponse> getAnomalies(Long userId) {
        return expenseRepository.findAnomaliesByUserId(userId)
                .stream()
                .map(ExpenseResponse::new)
                .collect(Collectors.toList());
    }

    public BigDecimal getTotalByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = expenseRepository.getTotalByUserAndDateRange(userId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<Object[]> getCategoryWiseTotals(Long userId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.getCategoryWiseTotals(userId, startDate, endDate);
    }
}
