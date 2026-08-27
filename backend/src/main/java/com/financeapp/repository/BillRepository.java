package com.financeapp.repository;

import com.financeapp.entity.Bill;
import com.financeapp.entity.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByUserIdOrderByDueDateAsc(Long userId);
    Optional<Bill> findByIdAndUserId(Long id, Long userId);
    List<Bill> findByUserIdAndDueDateBetweenOrderByDueDateAsc(Long userId, LocalDate start, LocalDate end);
    List<Bill> findByUserIdAndStatus(Long userId, BillStatus status);
}
