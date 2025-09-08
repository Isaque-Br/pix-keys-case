package br.com.itau.pixkeys.validation;

import br.com.itau.pixkeys.domain.KeyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneKeyValidatorTest {

    private final PhoneKeyValidator v = new PhoneKeyValidator();

    @Test
    void shouldAcceptExactly11Digits() {
        assertDoesNotThrow(() -> v.validate("+5511987654321"));
    }

    @Test
    void shouldAcceptExactly10Digits() {
        assertDoesNotThrow(() -> v.validate("+551138765432"));
    }

    @Test
    void shouldRejectTooShort_9Digits() {
        var ex = assertThrows(IllegalArgumentException.class, () -> v.validate("+55119876543"));
        assertEquals("telefone inválido (esperado +55 e 10-11 dígitos)", ex.getMessage());
    }

    @Test
    void shouldRejectTooLong_12Digits() {
        var ex = assertThrows(IllegalArgumentException.class, () -> v.validate("+55119876543210"));
        assertEquals("telefone inválido (esperado +55 e 10-11 dígitos)", ex.getMessage());
    }

    @Test
    void shouldRejectNull() {
        var ex = assertThrows(IllegalArgumentException.class, () -> v.validate(null));
        assertEquals("telefone não pode ser nulo", ex.getMessage());
    }

    @Test
    void shouldRejectBlank() {
        var ex = assertThrows(IllegalArgumentException.class, () -> v.validate("   "));
        assertEquals("telefone não pode ser vazio", ex.getMessage());
    }

    @Test
    void shouldRejectWhenNotStartingWithPlus55() {
        var ex = assertThrows(IllegalArgumentException.class, () -> v.validate("11987654321"));
        assertEquals("telefone deve iniciar com +55", ex.getMessage());
    }

    @Test
    void shouldRejectNonDigitsAfterPrefix() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> v.validate("+55 (11) 98765-4321"));
        assertEquals("telefone inválido (esperado +55 e 10-11 dígitos)", ex.getMessage());
    }

    @Test
    void supports_mustReturnPHONE() {
        assertEquals(KeyType.PHONE, v.supports());
    }
}
