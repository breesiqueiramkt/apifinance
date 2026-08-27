package com.financeapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "investment_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    // mapeado explicitamente pro nome de coluna "class" porque "class" é
    // palavra reservada em Java e não pode ser o nome do atributo
    @Enumerated(EnumType.STRING)
    @Column(name = "class", nullable = false, length = 30)
    private InvestmentClass investmentClass;
}
