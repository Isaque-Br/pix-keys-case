package br.com.itau.pixkeys.validation;
import br.com.itau.pixkeys.domain.KeyType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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

    @Test
    void shouldFail_whenValidatorsListIsNull_inConstructor() {
        var ex = assertThrows(NullPointerException.class, () -> new SimpleKeyValidatorFactory(null));
        assertTrue(ex.getMessage().toLowerCase().contains("lista de validadores"));
    }

    @Test
    void shouldFail_whenSupportsReturnsNull_inConstructor() {
        KeyValidator bad = new KeyValidator() {
            @Override
            public KeyType supports() {
                return null;
            }

            @Override
            public void validate(String value) {
            }
        };
        var ex = assertThrows(NullPointerException.class,
                () -> new SimpleKeyValidatorFactory(List.of(bad)));
        assertTrue(ex.getMessage().toLowerCase().contains("supports()"));
    }

    @Test
    void shouldFail_whenDuplicateTypeIsProvided_inConstructor() {
        var ex = assertThrows(IllegalStateException.class,
                () -> new SimpleKeyValidatorFactory(List.of(new EmailKeyValidator(), new EmailKeyValidator())));
        assertTrue(ex.getMessage().toLowerCase().contains("duplicado"));
    }

    @Test
    void shouldFail_whenAValidatorElementIsNull_inConstructor() {
        var list = new ArrayList<KeyValidator>();
        list.add(new EmailKeyValidator());
        list.add(null); // agora a lista aceita null
        var ex = assertThrows(NullPointerException.class,
                () -> new SimpleKeyValidatorFactory(list));
        assertTrue(ex.getMessage().toLowerCase().contains("validador não pode ser nulo"));
    }
}