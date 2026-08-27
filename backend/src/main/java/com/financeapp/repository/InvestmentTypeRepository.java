package com.financeapp.repository;

import com.financeapp.entity.InvestmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestmentTypeRepository extends JpaRepository<InvestmentType, Long> {
    List<InvestmentType> findAllByOrderByNameAsc();
    Optional<InvestmentType> findByNameIgnoreCase(String name);
}
