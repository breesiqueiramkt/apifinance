package com.financeapp.repository;

import com.financeapp.entity.FinancialSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinancialSettingsRepository extends JpaRepository<FinancialSettings, Long> {
    Optional<FinancialSettings> findByUserId(Long userId);
    Optional<FinancialSettings> findByUserIsNull();
}
