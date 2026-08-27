package com.financeapp.service;

import com.financeapp.dto.AccountDtos.AccountRequest;
import com.financeapp.dto.AccountDtos.AccountResponse;
import com.financeapp.entity.Account;
import com.financeapp.entity.User;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.AccountRepository;
import com.financeapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public List<AccountResponse> listForUser(Long userId) {
        return accountRepository.findByUserIdOrderByNameAsc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AccountResponse create(Long userId, AccountRequest request) {
        User userRef = userRepository.getReferenceById(userId);
        Account account = Account.builder()
                .user(userRef)
                .name(request.name())
                .bank(request.bank())
                .type(request.type())
                .balance(request.balance())
                .color(request.color() != null ? request.color() : "#1F6F54")
                .build();
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse update(Long userId, Long accountId, AccountRequest request) {
        Account account = findOwned(userId, accountId);
        account.setName(request.name());
        account.setBank(request.bank());
        account.setType(request.type());
        account.setBalance(request.balance());
        if (request.color() != null) account.setColor(request.color());
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public void delete(Long userId, Long accountId) {
        Account account = findOwned(userId, accountId);
        accountRepository.delete(account);
    }

    protected Account findOwned(Long userId, Long accountId) {
        return accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada ou não pertence a você"));
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(a.getId(), a.getName(), a.getBank(), a.getType(),
                a.getBalance(), a.getColor(), a.getCreatedAt());
    }
}
