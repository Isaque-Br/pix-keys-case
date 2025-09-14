package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomKeyValidatorTest {

    RandomKeyValidator v = new RandomKeyValidator();

    @Test
    void shouldAccept_exact32Alphanumeric_upperLowerDigits() {
        // 32 iguais (maiúsculas, minúsculas, dígitos)
        assertDoesNotThrow(() -> v.validate("A".repeat(32)));
        assertDoesNotThrow(() -> v.validate("a".repeat(32)));
        assertDoesNotThrow(() -> v.validate("1".repeat(32)));
        // misto: "Ab01" * 8 = 32
        assertDoesNotThrow(() -> v.validate("Ab01".repeat(8)));
    }

    @Test
    void shouldAccept_withSurroundingSpaces_whenInsideIs32Alphanumeric() {
        String core = "Ab01".repeat(8); // 32 chars válidos
        assertDoesNotThrow(() -> v.validate("   " + core + "   "));
    }

    @Test
    void shouldReject_null_orBlank() {
        assertThrows(BusinessRuleViolationException.class, () -> v.validate(null));
        assertThrows(BusinessRuleViolationException.class, () -> v.validate(""));
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("   "));
    }

    @Test
    void shouldReject_lengthDifferentFrom32() {
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("A".repeat(31))); // curto
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("A".repeat(33))); // longo
    }

    @Test
    void shouldReject_nonAlphanumericCharacters() {
        // hífen, underscore, acento e UUID com hífens
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("A".repeat(31) + "-"));
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("A".repeat(31) + "_"));
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("á".repeat(32)));
        assertThrows(BusinessRuleViolationException.class, () -> v.validate("550e8400-e29b-41d4-a716-446655440000"));
    }
}
