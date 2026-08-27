package com.financeapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Taxas estimadas usadas nas calculadoras e nas projeções de investimento. */
@Entity
@Table(name = "financial_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user; // null = linha default global (seed)

    @Column(name = "savings_rate", precision = 6, scale = 3)
    private BigDecimal savingsRate;

    @Column(name = "cdb_rate", precision = 6, scale = 3)
    private BigDecimal cdbRate;

    @Column(name = "treasury_rate", precision = 6, scale = 3)
    private BigDecimal treasuryRate;

    @Column(name = "fixed_income_rate", precision = 6, scale = 3)
    private BigDecimal fixedIncomeRate;

    @Column(name = "fii_rate", precision = 6, scale = 3)
    private BigDecimal fiiRate;

    @Column(name = "stocks_rate", precision = 6, scale = 3)
    private BigDecimal stocksRate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
