package br.com.itau.pixkeys.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomKeyValidatorTest {

    RandomKeyValidator v = new RandomKeyValidator();

    @Test
    void shouldAccept_alphanumeric_between1and36() {
        assertDoesNotThrow(() -> v.validate("a"));
        assertDoesNotThrow(() -> v.validate("A1b2C3"));
        assertDoesNotThrow(() -> v.validate("A".repeat(36)));
    }

    @Test
    void shouldAccept_withSurroundingSpaces() {
        assertDoesNotThrow(() -> v.validate("   AbC123   "));
    }

    @Test
    void shouldReject_null_blank_nonAlnum_orTooLong() {
        assertThrows(IllegalArgumentException.class, () -> v.validate(null));
        assertThrows(IllegalArgumentException.class, () -> v.validate(""));
        assertThrows(IllegalArgumentException.class, () -> v.validate("   "));
        assertThrows(IllegalArgumentException.class, () -> v.validate("abc-def"));
        assertThrows(IllegalArgumentException.class, () -> v.validate("ábc123"));
        assertThrows(IllegalArgumentException.class, () -> v.validate("a".repeat(37)));

        assertThrows(IllegalArgumentException.class, () ->
                v.validate("550e8400-e29b-41d4-a716-446655440000"));
    }
}
