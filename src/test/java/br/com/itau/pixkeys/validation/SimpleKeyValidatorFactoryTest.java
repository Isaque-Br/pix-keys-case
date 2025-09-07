package br.com.itau.pixkeys.validation;
import br.com.itau.pixkeys.domain.KeyType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleKeyValidatorFactoryTest {

    @Test
    void shouldReturnEmailValidator_whenTypeIsEMAIL() {
        var factory = new SimpleKeyValidatorFactory(List.of(new EmailKeyValidator()));
        var v = factory.get(KeyType.EMAIL);

        assertNotNull(v, "Factory deve retornar um validador");
        assertEquals(KeyType.EMAIL, v.supports(), "Validador deve declarar que suporta EMAIL");
        assertInstanceOf(EmailKeyValidator.class, v, "Deve ser a implementação de e-mail");

    }

    @Test
    void shouldFail_whenTypeIsNull_withPtBrMessage() {
        var factory = new SimpleKeyValidatorFactory(List.of(new EmailKeyValidator()));
        var ex = assertThrows(NullPointerException.class, () -> factory.get(null));

        assertEquals("KeyType não pode ser nulo", ex.getMessage());
    }

    @Test
    void shouldFail_whenTypeIsNotRegistered_withPtBrMessage() {
        var factory = new SimpleKeyValidatorFactory(List.of(new EmailKeyValidator()));

        var ex = assertThrows(IllegalStateException.class,
                () -> factory.get(KeyType.PHONE));

        assertEquals("Nenhum validador registrado para o tipo: PHONE", ex.getMessage());
    }
}
