package com.financeapp.service;

import com.financeapp.dto.CreditCardDtos.PurchaseRequest;
import com.financeapp.entity.CreditCard;
import com.financeapp.entity.CreditCardTransaction;
import com.financeapp.entity.User;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.CreditCardRepository;
import com.financeapp.repository.CreditCardTransactionRepository;
import com.financeapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CreditCardServiceTest {

    @Mock private CreditCardRepository creditCardRepository;
    @Mock private CreditCardTransactionRepository ccTransactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;

    private CreditCardService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CreditCardService(creditCardRepository, ccTransactionRepository, categoryRepository, userRepository);
    }

    @Test
    void splitsPurchaseIntoInstallmentsThatSumExactlyToTheOriginalAmount() {
        User user = User.builder().id(1L).build();
        CreditCard card = CreditCard.builder()
                .id(5L).user(user).name("Nubank").creditLimit(new BigDecimal("5000"))
                .closingDay((short) 10).dueDay((short) 17).build();

        when(creditCardRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(card));
        when(ccTransactionRepository.save(any(CreditCardTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        // 100,00 dividido em 3 parcelas: 33.33 + 33.33 + 33.34 (ajuste na última) = 100.00 exato
        PurchaseRequest request = new PurchaseRequest(
                "Notebook", new BigDecimal("100.00"), LocalDate.of(2026, 1, 15), null, (short) 3);

        var result = service.addPurchase(1L, 5L, request);

        BigDecimal sum = result.stream().map(r -> r.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("100.00");
        assertThat(result.get(0).amount()).isEqualByComparingTo("33.33");
        assertThat(result.get(1).amount()).isEqualByComparingTo("33.33");
        assertThat(result.get(2).amount()).isEqualByComparingTo("33.34"); // sobra do arredondamento vai pra última parcela
    }

    @Test
    void eachInstallmentIsProjectedOneMonthAfterThePrevious() {
        User user = User.builder().id(1L).build();
        CreditCard card = CreditCard.builder()
                .id(5L).user(user).name("Nubank").creditLimit(new BigDecimal("5000"))
                .closingDay((short) 10).dueDay((short) 17).build();

        when(creditCardRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(card));
        when(ccTransactionRepository.save(any(CreditCardTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseRequest request = new PurchaseRequest(
                "Geladeira", new BigDecimal("300.00"), LocalDate.of(2026, 3, 5), null, (short) 3);
        var result = service.addPurchase(1L, 5L, request);

        assertThat(result.get(0).purchaseDate()).isEqualTo(LocalDate.of(2026, 3, 5));
        assertThat(result.get(1).purchaseDate()).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(result.get(2).purchaseDate()).isEqualTo(LocalDate.of(2026, 5, 5));
    }
}
