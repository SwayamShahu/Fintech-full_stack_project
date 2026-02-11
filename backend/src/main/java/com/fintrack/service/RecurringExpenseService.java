package com.fintrack.service;

import com.fintrack.dto.RecurringExpenseRequest;
import com.fintrack.dto.RecurringExpenseResponse;
import com.fintrack.model.Category;
import com.fintrack.model.Expense;
import com.fintrack.model.Frequency;
import com.fintrack.model.RecurringExpense;
import com.fintrack.model.User;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.ExpenseRepository;
import com.fintrack.repository.RecurringExpenseRepository;
import com.fintrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public RecurringExpenseResponse createRecurringExpense(Long userId, RecurringExpenseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        RecurringExpense recurringExpense = RecurringExpense.builder()
                .user(user)
                .amount(request.getAmount())
                .category(category)
                .description(request.getDescription())
                .frequency(request.getFrequency())
                .nextDueDate(request.getStartDate())
                .isActive(true)
                .build();

        recurringExpense = recurringExpenseRepository.save(recurringExpense);
        return new RecurringExpenseResponse(recurringExpense);
    }

    public List<RecurringExpenseResponse> getActiveRecurringExpenses(Long userId) {
        return recurringExpenseRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(RecurringExpenseResponse::new)
                .collect(Collectors.toList());
    }

    public List<RecurringExpenseResponse> getAllRecurringExpenses(Long userId) {
        return recurringExpenseRepository.findByUserIdOrderByNextDueDateAsc(userId)
                .stream()
                .map(RecurringExpenseResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public RecurringExpenseResponse updateRecurringExpense(Long userId, Long id, RecurringExpenseRequest request) {
        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring expense not found"));

        if (!recurringExpense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        recurringExpense.setAmount(request.getAmount());
        recurringExpense.setCategory(category);
        recurringExpense.setDescription(request.getDescription());
        recurringExpense.setFrequency(request.getFrequency());
        recurringExpense.setNextDueDate(request.getStartDate());

        recurringExpense = recurringExpenseRepository.save(recurringExpense);
        return new RecurringExpenseResponse(recurringExpense);
    }

    @Transactional
    public void deactivateRecurringExpense(Long userId, Long id) {
        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring expense not found"));

        if (!recurringExpense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        recurringExpense.setIsActive(false);
        recurringExpenseRepository.save(recurringExpense);
    }

    @Transactional
    public void deleteRecurringExpense(Long userId, Long id) {
        RecurringExpense recurringExpense = recurringExpenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recurring expense not found"));

        if (!recurringExpense.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }

        recurringExpenseRepository.delete(recurringExpense);
    }

    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    @Transactional
    public void processRecurringExpenses() {
        log.info("Processing recurring expenses...");
        LocalDate today = LocalDate.now();
        List<RecurringExpense> dueExpenses = recurringExpenseRepository
                .findByNextDueDateLessThanEqualAndIsActiveTrue(today);

        for (RecurringExpense recurring : dueExpenses) {
            // Create actual expense
            Expense expense = Expense.builder()
                    .user(recurring.getUser())
                    .amount(recurring.getAmount())
                    .category(recurring.getCategory())
                    .description(recurring.getDescription() + " (Recurring)")
                    .expenseDate(recurring.getNextDueDate())
                    .isRecurring(true)
                    .build();

            expenseRepository.save(expense);

            // Update next due date
            recurring.setNextDueDate(calculateNextDueDate(recurring.getNextDueDate(), recurring.getFrequency()));
            recurringExpenseRepository.save(recurring);

            log.info("Processed recurring expense ID: {} for user: {}", 
                    recurring.getId(), recurring.getUser().getId());
        }
    }

    private LocalDate calculateNextDueDate(LocalDate currentDate, Frequency frequency) {
        return switch (frequency) {
            case DAILY -> currentDate.plusDays(1);
            case WEEKLY -> currentDate.plusWeeks(1);
            case MONTHLY -> currentDate.plusMonths(1);
            case YEARLY -> currentDate.plusYears(1);
        };
    }

    public List<RecurringExpenseResponse> getUpcomingDueExpenses(Long userId, int days) {
        LocalDate endDate = LocalDate.now().plusDays(days);
        return recurringExpenseRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .filter(e -> !e.getNextDueDate().isAfter(endDate))
                .map(RecurringExpenseResponse::new)
                .collect(Collectors.toList());
    }
}
