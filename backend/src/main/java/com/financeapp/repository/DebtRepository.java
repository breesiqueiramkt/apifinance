package com.financeapp.repository;

import com.financeapp.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DebtRepository extends JpaRepository<Debt, Long> {

    // método "avalanche": maior taxa de juros primeiro. Dívidas sem taxa
    // informada vão para o final (NULLS LAST), em vez de "furarem a fila"
    // por causa do comportamento padrão de NULL do banco.
    @Query("""
           SELECT d FROM Debt d WHERE d.user.id = :userId
           ORDER BY d.interestRate DESC NULLS LAST, d.currentAmount DESC
           """)
    List<Debt> findByUserIdOrderByInterestRateDescCurrentAmountDesc(@Param("userId") Long userId);

    Optional<Debt> findByIdAndUserId(Long id, Long userId);
}
