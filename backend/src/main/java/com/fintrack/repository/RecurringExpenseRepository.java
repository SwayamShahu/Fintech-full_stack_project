package com.fintrack.repository;

import com.fintrack.model.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {
    
    List<RecurringExpense> findByUserIdAndIsActiveTrue(Long userId);
    
    List<RecurringExpense> findByUserId(Long userId);
    
    List<RecurringExpense> findByNextDueDateLessThanEqualAndIsActiveTrue(LocalDate date);
    
    List<RecurringExpense> findByUserIdOrderByNextDueDateAsc(Long userId);
}
