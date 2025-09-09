package br.com.itau.pixkeys.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CnpjKeyValidatorTest {

    CnpjKeyValidator v = new CnpjKeyValidator();

    @Test
    void shouldAccept_validCnpj_withOrWithoutMask() {
        assertDoesNotThrow(() -> v.validate("12.345.678/0001-95"));
        assertDoesNotThrow(() -> v.validate("12345678000195"));
    }

    @Test
    void shouldReject_wrongLength_orRepeatedDigits_orWrongDigits() {
        assertThrows(IllegalArgumentException.class, () -> v.validate("12.345.678/0001-9"));
        assertThrows(IllegalArgumentException.class, () -> v.validate("00000000000000"));
        assertThrows(IllegalArgumentException.class, () -> v.validate("12.345.678/0001-96"));
    }
}
