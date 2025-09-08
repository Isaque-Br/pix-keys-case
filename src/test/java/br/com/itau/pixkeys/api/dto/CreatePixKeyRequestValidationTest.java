package br.com.itau.pixkeys.api.dto;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreatePixKeyRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private CreatePixKeyRequest valid() {
        return new CreatePixKeyRequest(
                KeyType.EMAIL, "ana@exemplo.com", AccountType.SAVINGS,
                "1234", "00001234", "Ana", "Silva"
        );
    }

    private static boolean hasViolation(Set<? extends ConstraintViolation<?>> v, String property) {
        return v.stream().anyMatch(cv -> property.equals(cv.getPropertyPath().toString()));
    }

    @Test
    void shouldBeValid_whenAllFieldsAreOk() {
        var req = valid();
        var violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "DTO válido não deve ter violações");
    }

    @Test
    void shouldFail_whenKeyTypeIsNull() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                null, ok.keyValue(), ok.accountType(),
                ok.agency(), ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "keyType"));
    }

    @Test
    void shouldFail_whenAccountTypeIsNull() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), ok.keyValue(), null,
                ok.agency(), ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "accountType"));
    }

    @Test
    void shouldFail_whenKeyValueIsBlank() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), "   ", ok.accountType(),
                ok.agency(), ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "keyValue"));
    }

    @Test
    void shouldAccept_whenKeyValueHas77Chars() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), "a".repeat(77), ok.accountType(),
                ok.agency(), ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(validator.validate(req).isEmpty(), "77 deve ser permitido por @Size(max=77)");
    }

    @Test
    void shouldFail_whenKeyValueExceeds77() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), "a".repeat(78), ok.accountType(),
                ok.agency(), ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "keyValue"));
    }

    @Test
    void shouldFail_whenAgencyHasWrongFormat() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), ok.keyValue(), ok.accountType(),
                "12A4", ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "agency"));
    }

    @Test
    void shouldFail_whenAccountHasWrongFormat() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), ok.keyValue(), ok.accountType(),
                ok.agency(), "12", ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "account"));
    }

    @Test
    void shouldAllowNullHolderSurname_butEnforceMax45() {
        var okNull = new CreatePixKeyRequest(
                KeyType.EMAIL, "ana@exemplo.com", AccountType.SAVINGS,
                "1234", "00001234", "Ana", null
        );
        assertTrue(validator.validate(okNull).isEmpty(), "sobrenome nulo deve ser aceito");

        var bad = new CreatePixKeyRequest(
                KeyType.EMAIL, "ana@exemplo.com", AccountType.SAVINGS,
                "1234", "00001234", "Ana", "x".repeat(46)
        );
        assertTrue(hasViolation(validator.validate(bad), "holderSurname"));
    }
}
