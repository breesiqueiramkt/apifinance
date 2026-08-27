package com.financeapp.repository;

import com.financeapp.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserIdOrderByNameAsc(Long userId);
    Optional<Account> findByIdAndUserId(Long id, Long userId);

    /**
     * Mesma busca de findByIdAndUserId, mas trava a linha no banco (SELECT ...
     * FOR UPDATE) até a transação terminar. Usada sempre que o saldo da conta
     * vai ser lido e depois somado/subtraído (ver TransactionService) - sem
     * isso, você e sua esposa lançando algo quase ao mesmo tempo na mesma
     * conta poderiam fazer uma atualização "sumir" (o clássico lost update).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id AND a.user.id = :userId")
    Optional<Account> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
