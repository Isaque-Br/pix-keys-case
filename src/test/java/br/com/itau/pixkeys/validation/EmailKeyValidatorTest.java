package br.com.itau.pixkeys.validation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailKeyValidatorTest {

    private final EmailKeyValidator v = new EmailKeyValidator();

    @Test
    void shouldAcceptValidEmail() {
        assertDoesNotThrow(() -> v.validate("ana.silva+pix@exemplo.com"));
    }

    @Test
    void shouldRejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> v.validate("   "));
    }

    @Test
    void shouldRejectInvalidFormat() {
       var ex = assertThrows(IllegalArgumentException.class, () -> v.validate("ana@exemplo"));
        System.out.println("Mensagem da exceção: " + ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> v.validate("ana@@exemplo.com"));
        assertThrows(IllegalArgumentException.class, () -> v.validate("@exemplo.com"));
    }

    @Test
    void shouldRejectEmailLongerThan77() {
        String tooLong = "a".repeat(73) + "@x.com";
        assertThrows(IllegalArgumentException.class, () -> v.validate(tooLong));
    }

    @Test
    void shouldAcceptExactly77Chars() {
        String boundary = "a".repeat(71) + "@x.com";
        assertDoesNotThrow(() -> v.validate(boundary));
    }
}
