package com.financeapp.exception;

/** Erros de regra de negócio (ex: e-mail já cadastrado, limite excedido, etc.) */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
