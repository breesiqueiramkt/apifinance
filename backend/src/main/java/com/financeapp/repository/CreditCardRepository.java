package com.financeapp.repository;

import com.financeapp.entity.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {
    List<CreditCard> findByUserIdOrderByNameAsc(Long userId);
    Optional<CreditCard> findByIdAndUserId(Long id, Long userId);
}
