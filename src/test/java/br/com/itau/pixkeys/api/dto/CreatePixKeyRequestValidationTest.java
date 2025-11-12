package br.com.itau.pixkeys.api.dto;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class CreatePixKeyRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private CreatePixKeyRequest valid() {
        return new CreatePixKeyRequest(
                KeyType.EMAIL, "ana@exemplo.com", AccountType.SAVINGS,
                "1234", "00001234", "Ana", "Silva"
        );
    }

    /** Helper: verifica se há violação para um propertyPath específico,
     *  opcionalmente filtrando por mensagem. */
    private static boolean hasViolation(Set<? extends ConstraintViolation<?>> v,
                                        String property,
                                        Predicate<String> messageFilter) {
        return v.stream().anyMatch(cv ->
                property.equals(cv.getPropertyPath().toString()) &&
                        (messageFilter == null || messageFilter.test(cv.getMessage()))
        );
    }

    private static boolean hasViolation(Set<? extends ConstraintViolation<?>> v, String property) {
        return hasViolation(v, property, null);
    }

    @Test
    @DisplayName("DTO válido não deve produzir violações")
    void shouldBeValid_whenAllFieldsAreOk() {
        var req = valid();
        var violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "DTO válido não deve ter violações");
    }

    @Test
    @DisplayName("keyType é obrigatório")
    void shouldFail_whenKeyTypeIsNull() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                null, ok.keyValue(), ok.accountType(),
                ok.agency(), ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "keyType"));
    }

    @Test
    @DisplayName("accountType é obrigatório")
    void shouldFail_whenAccountTypeIsNull() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), ok.keyValue(), null,
                ok.agency(), ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "accountType"));
    }

    @Test
    @DisplayName("keyValue não pode ser em branco")
    void shouldFail_whenKeyValueIsBlank() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), "   ", ok.accountType(),
                ok.agency(), ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "keyValue"));
    }

    @Test
    @DisplayName("keyValue aceita até 77 caracteres (limite superior)")
    void shouldAccept_whenKeyValueHas77Chars() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), "a".repeat(77), ok.accountType(),
                ok.agency(), ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(validator.validate(req).isEmpty(), "77 deve ser permitido por @Size(max=77)");
    }

    @Test
    @DisplayName("keyValue acima de 77 caracteres deve falhar")
    void shouldFail_whenKeyValueExceeds77() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), "a".repeat(78), ok.accountType(),
                ok.agency(), ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "keyValue"));
    }

    @Test
    @DisplayName("agency deve ter 4 dígitos (regex)")
    void shouldFail_whenAgencyHasWrongFormat() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), ok.keyValue(), ok.accountType(),
                "12A4", ok.account(), ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "agency"));
    }

    @Test
    @DisplayName("account deve respeitar o formato (ex.: 8 dígitos)")
    void shouldFail_whenAccountHasWrongFormat() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), ok.keyValue(), ok.accountType(),
                ok.agency(), "12", ok.holderName(), ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "account"));
    }

    @Test
    @DisplayName("holderSurname pode ser nulo; se presente, respeita @Size(max=45)")
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

    @Test
    @DisplayName("holderName é obrigatório")
    void shouldFail_whenHolderNameBlank() {
        var ok = valid();
        var req = new CreatePixKeyRequest(
                ok.keyType(), ok.keyValue(), ok.accountType(),
                ok.agency(), ok.account(), "   ", ok.holderSurname()
        );
        assertTrue(hasViolation(validator.validate(req), "holderName"));
    }
}
