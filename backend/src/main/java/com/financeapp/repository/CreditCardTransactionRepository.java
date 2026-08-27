package com.financeapp.repository;

import com.financeapp.entity.CreditCardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CreditCardTransactionRepository extends JpaRepository<CreditCardTransaction, Long> {

    List<CreditCardTransaction> findByCreditCardIdOrderByPurchaseDateDesc(Long creditCardId);

    List<CreditCardTransaction> findByCreditCardIdAndPurchaseDateBetween(
            Long creditCardId, LocalDate start, LocalDate end);

    @Query("""
           SELECT COALESCE(SUM(t.amount), 0) FROM CreditCardTransaction t
           WHERE t.creditCard.id = :cardId AND t.purchaseDate BETWEEN :start AND :end
           """)
    BigDecimal sumByCardAndPeriod(@Param("cardId") Long cardId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    // soma de todas as parcelas futuras (ainda não viradas em fatura passada) = limite comprometido
    @Query("""
           SELECT COALESCE(SUM(t.amount), 0) FROM CreditCardTransaction t
           WHERE t.creditCard.id = :cardId AND t.purchaseDate >= :from
           """)
    BigDecimal sumOutstandingFromDate(@Param("cardId") Long cardId, @Param("from") LocalDate from);
}
