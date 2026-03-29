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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final MLAnomalyService mlAnomalyService;

    @Transactional
    public ExpenseResponse createExpense(Long userId, ExpenseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Try ML-based detection first, fallback to rule-based
        AnomalyResult anomalyResult = detectAnomaliesWithML(userId, request, category.getName());

        Expense expense = Expense.builder()
                .user(user)
                .amount(request.getAmount())
                .category(category)
                .description(request.getDescription())
                .expenseDate(request.getExpenseDate())
                .paymentMode(request.getPaymentMode())
                .isRecurring(request.getIsRecurring())
                .isAnomaly(anomalyResult.isAnomaly)
                .anomalyScore(anomalyResult.score)
                .anomalyExplanation(anomalyResult.explanation)
                .anomalyType(anomalyResult.type)
                .build();

        expense = expenseRepository.save(expense);
        
        // Trigger ML model update if user has enough transactions
        triggerMLTrainingIfNeeded(userId);
        
        return new ExpenseResponse(expense);
    }
    
    /**
     * Detect anomalies using ML service with rule-based fallback
     */
    private AnomalyResult detectAnomaliesWithML(Long userId, ExpenseRequest request, String categoryName) {
        // Try ML service first
        try {
            MLAnomalyService.MLAnomalyResult mlResult = mlAnomalyService.detectAnomaly(
                userId,
                request.getAmount(),
                categoryName,
                request.getExpenseDate(),
                request.getPaymentMode()
            );
            
            if (mlResult.usedMlService) {
                logger.info("Using ML-based anomaly detection for user {}", userId);
                AnomalyResult result = new AnomalyResult();
                result.isAnomaly = mlResult.isAnomaly;
                result.score = mlResult.score;
                result.explanation = mlResult.explanation;
                result.type = mlResult.type;
                return result;
            }
        } catch (Exception e) {
            logger.warn("ML anomaly detection failed, using rule-based: {}", e.getMessage());
        }
        
        // Fallback to rule-based detection
        logger.debug("Using rule-based anomaly detection for user {}", userId);
        return detectAnomalies(userId, request);
    }
    
    /**
     * Trigger ML model training when user reaches milestones
     */
    private void triggerMLTrainingIfNeeded(Long userId) {
        Long count = expenseRepository.countByUserId(userId);
        // Train personal model at 10, 25, 50, 100 transactions
        if (count != null && (count == 10 || count == 25 || count == 50 || count == 100)) {
            logger.info("Triggering ML model training for user {} at {} transactions", userId, count);
            mlAnomalyService.triggerUserTraining(userId);
        }
    }

    // Inner class to hold anomaly detection results
    private static class AnomalyResult {

        boolean isAnomaly = false;
        BigDecimal score = BigDecimal.ZERO;
        String explanation = null;
        String type = null;
    }

    private static final BigDecimal MAX_ANOMALY_SCORE = new BigDecimal("9.9999");

    private AnomalyResult detectAnomalies(Long userId, ExpenseRequest request) {
        AnomalyResult result = new AnomalyResult();
        java.util.List<String> anomalyReasons = new java.util.ArrayList<>();

        // 1. HIGH COST ANOMALY - expense significantly higher than overall average
        BigDecimal overallAverage = expenseRepository.getAverageByUserId(userId);
        if (overallAverage != null && overallAverage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costThreshold = overallAverage.multiply(BigDecimal.valueOf(2.5));
            if (request.getAmount().compareTo(costThreshold) > 0) {
                BigDecimal ratio = request.getAmount().divide(overallAverage, 4, RoundingMode.HALF_UP);
                result.isAnomaly = true;
                result.score = ratio.min(MAX_ANOMALY_SCORE);
                result.type = "HIGH_COST";
                anomalyReasons.add("Amount is " + ratio.setScale(1, RoundingMode.HALF_UP) + "x your average spending");
            }
        }

        // 2. CATEGORY ANOMALY - expense much higher than category-specific average
        BigDecimal categoryAverage = expenseRepository.getAverageByCategoryAndUserId(userId, request.getCategoryId());
        Long categoryCount = expenseRepository.countByCategoryAndUserId(userId, request.getCategoryId());
        if (categoryAverage != null && categoryCount != null && categoryCount >= 3 && categoryAverage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal categoryThreshold = categoryAverage.multiply(BigDecimal.valueOf(2));
            if (request.getAmount().compareTo(categoryThreshold) > 0) {
                BigDecimal ratio = request.getAmount().divide(categoryAverage, 4, RoundingMode.HALF_UP);
                if (!result.isAnomaly || ratio.compareTo(result.score) > 0) {
                    result.isAnomaly = true;
                    result.score = ratio.min(MAX_ANOMALY_SCORE);
                    result.type = "CATEGORY_SPIKE";
                }
                anomalyReasons.add("Amount is " + ratio.setScale(1, RoundingMode.HALF_UP) + "x your average for this category");
            }
        }

        // 3. FREQUENCY ANOMALY - multiple expenses in same category on same day
        Long sameDayCount = expenseRepository.countSameDaySameCategory(userId, request.getCategoryId(), request.getExpenseDate());
        if (sameDayCount != null && sameDayCount >= 2) {
            result.isAnomaly = true;
            if (result.type == null) {
                result.type = "HIGH_FREQUENCY";
                result.score = BigDecimal.valueOf(Math.min(sameDayCount + 1, 9)).setScale(4, RoundingMode.HALF_UP);
            }
            anomalyReasons.add("You have " + (sameDayCount + 1) + " expenses in this category today");
        }

        // 4. DUPLICATE DETECTION - similar amount in same category on same day
        BigDecimal duplicateThreshold = request.getAmount().multiply(BigDecimal.valueOf(0.1)); // Within 10%
        List<Expense> potentialDuplicates = expenseRepository.findPotentialDuplicates(
                userId, request.getExpenseDate(), request.getCategoryId(), request.getAmount(), duplicateThreshold);
        if (potentialDuplicates != null && !potentialDuplicates.isEmpty()) {
            result.isAnomaly = true;
            if (result.type == null) {
                result.type = "POTENTIAL_DUPLICATE";
                result.score = BigDecimal.valueOf(Math.min(potentialDuplicates.size() + 1, 9)).setScale(4, RoundingMode.HALF_UP);
            }
            anomalyReasons.add("Similar expense already recorded today (potential duplicate)");
        }

        // 5. UNUSUAL FREQUENCY IN WEEK - too many expenses in same category in past 7 days
        LocalDate weekAgo = request.getExpenseDate().minusDays(7);
        Long weekCount = expenseRepository.countByUserAndCategoryInDateRange(
                userId, request.getCategoryId(), weekAgo, request.getExpenseDate());
        if (weekCount != null && weekCount >= 5) {
            if (!result.isAnomaly) {
                result.isAnomaly = true;
                result.type = "WEEKLY_FREQUENCY_SPIKE";
                result.score = BigDecimal.valueOf(Math.min(weekCount + 1, 9)).setScale(4, RoundingMode.HALF_UP);
            }
            anomalyReasons.add("You have made " + (weekCount + 1) + " expenses in this category this week");
        }

        // 6. UNUSUAL/RARE CATEGORY - spending in a category rarely used
        Long totalExpenses = expenseRepository.countByUserId(userId);
        Long categoryExpenses = expenseRepository.countByCategoryAndUserId(userId, request.getCategoryId());
        if (totalExpenses != null && totalExpenses >= 5 && categoryExpenses != null) {
            double categoryPercentage = (double) categoryExpenses / totalExpenses * 100;
            // If this category represents less than 10% of expenses, it's unusual
            if (categoryPercentage < 10) {
                result.isAnomaly = true;
                if (result.type == null) {
                    result.type = "UNUSUAL_CATEGORY";
                    // Score on 0-9.99 scale (inverted percentage / 10)
                    double scoreValue = (10 - categoryPercentage) / 10 * 9.99;
                    result.score = BigDecimal.valueOf(scoreValue).setScale(4, RoundingMode.HALF_UP);
                }
                if (categoryExpenses == 0) {
                    anomalyReasons.add("First expense in this category - unusual spending pattern");
                } else {
                    anomalyReasons.add("Rare category - only " + String.format("%.0f", categoryPercentage) + "% of your expenses are in this category");
                }
            }
        }

        // Combine all anomaly reasons into explanation
        if (!anomalyReasons.isEmpty()) {
            result.explanation = String.join("; ", anomalyReasons);
        }

        return result;
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
