package com.financeapp.repository;

import com.financeapp.entity.Transaction;
import com.financeapp.entity.TransactionStatus;
import com.financeapp.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate start, LocalDate end);

    List<Transaction> findByUserIdOrderByDateDesc(Long userId);

    @Query("""
           SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
           WHERE t.user.id = :userId AND t.type = :type
           AND t.date BETWEEN :start AND :end
           """)
    BigDecimal sumByUserAndTypeAndPeriod(@Param("userId") Long userId,
                                          @Param("type") TransactionType type,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);

    @Query("""
           SELECT t.category.id, COALESCE(SUM(t.amount), 0) FROM Transaction t
           WHERE t.user.id = :userId AND t.type = :type
           AND t.date BETWEEN :start AND :end
           GROUP BY t.category.id
           """)
    List<Object[]> sumGroupedByCategory(@Param("userId") Long userId,
                                         @Param("type") TransactionType type,
                                         @Param("start") LocalDate start,
                                         @Param("end") LocalDate end);

    @Query("""
           SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
           WHERE t.user.id = :userId AND t.type = :type AND t.status IN :statuses
           AND t.date BETWEEN :start AND :end
           """)
    BigDecimal sumByUserAndTypeAndStatusesAndPeriod(@Param("userId") Long userId,
                                                      @Param("type") TransactionType type,
                                                      @Param("statuses") List<TransactionStatus> statuses,
                                                      @Param("start") LocalDate start,
                                                      @Param("end") LocalDate end);

    List<Transaction> findByUserIdAndStatusInAndDateBetweenOrderByDateAsc(
            Long userId, List<TransactionStatus> statuses, LocalDate start, LocalDate end);
}
