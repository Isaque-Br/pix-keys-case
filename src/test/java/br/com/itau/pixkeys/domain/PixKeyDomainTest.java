package br.com.itau.pixkeys.domain;

import br.com.itau.pixkeys.domain.model.PixKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixKeyDomainTest {

    @Test
    @DisplayName("holderSurname: null vira \"\" e valor não-nulo é preservado")
    void holderSurname_nullBecomesEmpty_and_nonNullPreserved() {
        PixKey withNull = PixKey.create(
                KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", null // <- força ramo do sanitizeOptional(null)
        );
        assertEquals("", withNull.holderSurname());

        PixKey withSurname = PixKey.create(
                KeyType.EMAIL, "c@d.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        assertEquals("Silva", withSurname.holderSurname());
    }

    @Test
    @DisplayName("inactivate: primeira vez ok; segunda vez lança exceção de regra")
    void inactivate_then_cannotInactivateAgain() {
        PixKey active = PixKey.create(
                KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        PixKey inactive = active.inactivate(); // ramo OK
        assertTrue(inactive.isInactive());

        // agora aciona o outro ramo (erro)
        assertThrows(BusinessRuleViolationException.class, inactive::inactivate);
    }
}
