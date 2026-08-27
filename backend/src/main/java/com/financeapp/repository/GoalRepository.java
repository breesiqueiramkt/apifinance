package com.financeapp.repository;

import com.financeapp.entity.Goal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserIdOrderByDeadlineAsc(Long userId);
    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    /** Trava a linha (SELECT ... FOR UPDATE) para somar um aporte com segurança - ver AccountRepository.findByIdAndUserIdForUpdate. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Goal g WHERE g.id = :id AND g.user.id = :userId")
    Optional<Goal> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
