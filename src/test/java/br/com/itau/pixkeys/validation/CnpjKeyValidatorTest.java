package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CnpjKeyValidatorTest {

    CnpjKeyValidator v = new CnpjKeyValidator();

    @Test
    void shouldReject_null_orBlank() {
        assertThrows(BusinessRuleViolationException.class, () -> v.validate(null));
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("   "));
    }

    @Test
    void shouldAccept_validCnpj_withOrWithoutMask() {
        assertDoesNotThrow(() -> v.validate("12.345.678/0001-95"));
        assertDoesNotThrow(() -> v.validate("12345678000195"));
    }

    @Test
    void shouldReject_wrongLength_orRepeatedDigits_orWrongDigits() {
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("12.345.678/0001-9"));  // 13 dígitos
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("00000000000000"));     // todos iguais
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("12.345.678/0001-96")); // DV inválido
    }

    @Test
    void shouldReject_disallowedCharacters() {
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("12.345.678/0001-9A"));
    }
}
