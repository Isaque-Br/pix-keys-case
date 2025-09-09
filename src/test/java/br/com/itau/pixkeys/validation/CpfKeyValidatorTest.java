package br.com.itau.pixkeys.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpfKeyValidatorTest {

    CpfKeyValidator v = new CpfKeyValidator();

    @Test
    void shouldAccept_validCpf_withOrWithoutMask() {
        assertDoesNotThrow(() -> v.validate("529.982.247-25")); // válido
        assertDoesNotThrow(() -> v.validate("12345678909"));    // válido
    }

    @Test
    void shouldReject_wrongLength_orRepeatedDigits_orWrongDigits() {
        assertThrows(IllegalArgumentException.class, () -> v.validate("123.456.789-0")); // curto
        assertThrows(IllegalArgumentException.class, () -> v.validate("00000000000"));   // repetido
        assertThrows(IllegalArgumentException.class, () -> v.validate("529.982.247-24"));// DV errado
    }
}
