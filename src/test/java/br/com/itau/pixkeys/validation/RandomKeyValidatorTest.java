package br.com.itau.pixkeys.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomKeyValidatorTest {

    RandomKeyValidator v = new RandomKeyValidator();

    @Test
    void shouldAccept_validUuid() {
        String v4 = java.util.UUID.randomUUID().toString();
        assertDoesNotThrow(() -> v.validate(v4));
    }

    @Test
    void shouldReject_blank_orBadFormat() {
        assertThrows(IllegalArgumentException.class, () -> v.validate(""));
        assertThrows(IllegalArgumentException.class, () -> v.validate("not-a-uuid"));
        assertThrows(IllegalArgumentException.class, () -> v.validate("123e4567e89b12d3a456426614174000"));
    }

    @Test
    void shouldReject_nonV4() {
        String v3 = java.util.UUID.nameUUIDFromBytes("x".getBytes()).toString();
        assertThrows(IllegalArgumentException.class, () -> v.validate(v3));
    }

    @Test
    void shouldAccept_uppercase_andSurroundingSpaces() {
        String u = java.util.UUID.randomUUID().toString();
        assertDoesNotThrow(() -> v.validate("  " + u.toUpperCase() + "  "));
    }

    @Test
    void shouldReject_null() {
        assertThrows(IllegalArgumentException.class, () -> v.validate(null));
    }
}
