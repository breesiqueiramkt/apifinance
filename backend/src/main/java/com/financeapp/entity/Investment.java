package com.financeapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_type_id")
    private InvestmentType investmentType;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "invested_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal investedAmount;

    @Column(name = "current_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal currentAmount;

    @Column(name = "invested_at", nullable = false)
    private LocalDate investedAt;

    @Column(name = "expected_rate", precision = 6, scale = 3)
    private BigDecimal expectedRate; // % ao ano, estimado

    @Column(length = 120)
    private String institution;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
