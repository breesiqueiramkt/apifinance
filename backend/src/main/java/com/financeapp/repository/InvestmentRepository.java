package com.financeapp.repository;

import com.financeapp.entity.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserIdOrderByInvestedAtDesc(Long userId);
    Optional<Investment> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(i.currentAmount), 0) FROM Investment i WHERE i.user.id = :userId")
    BigDecimal sumCurrentAmountByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(i.investedAmount), 0) FROM Investment i WHERE i.user.id = :userId")
    BigDecimal sumInvestedAmountByUser(@Param("userId") Long userId);
}
