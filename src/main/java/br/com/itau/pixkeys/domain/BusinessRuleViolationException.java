package br.com.itau.pixkeys.domain;

/** Exceção de domínio para violações de regra de negócio (mapeada para HTTP 422). */
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}

