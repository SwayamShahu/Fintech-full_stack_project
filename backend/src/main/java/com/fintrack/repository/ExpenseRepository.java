package com.fintrack.repository;

import com.fintrack.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    List<Expense> findByUserIdOrderByExpenseDateDesc(Long userId);
    
    List<Expense> findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(
        Long userId, LocalDate startDate, LocalDate endDate);
    
    List<Expense> findByUserIdAndCategoryIdOrderByExpenseDateDesc(Long userId, Long categoryId);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId " +
           "AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalByUserAndDateRange(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e.category.name, SUM(e.amount) FROM Expense e " +
           "WHERE e.user.id = :userId AND e.expenseDate BETWEEN :startDate AND :endDate " +
           "GROUP BY e.category.name ORDER BY SUM(e.amount) DESC")
    List<Object[]> getCategoryWiseTotals(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId AND e.isAnomaly = true " +
           "ORDER BY e.expenseDate DESC")
    List<Expense> findAnomaliesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(e) FROM Expense e WHERE e.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT AVG(e.amount) FROM Expense e WHERE e.user.id = :userId")
    BigDecimal getAverageByUserId(@Param("userId") Long userId);
}
