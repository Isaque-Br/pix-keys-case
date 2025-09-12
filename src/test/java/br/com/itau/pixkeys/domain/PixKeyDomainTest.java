package br.com.itau.pixkeys.domain;

import br.com.itau.pixkeys.domain.model.PixKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixKeyDomainTest {

    @Test
    void nzs_null_becomesEmpty_and_nonNullPreserved() {
        PixKey withNull = PixKey.create(
                KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", null // <- força ramo do nzs(null)
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
    void inactivate_then_cannotInactivateAgain() {
        PixKey active = PixKey.create(
                KeyType.EMAIL, "a@b.com",
                AccountType.CHECKING, "1250", "00001234",
                "Ana", "Silva"
        );
        PixKey inactive = active.inactivate(); // ramo “OK”
        assertTrue(inactive.isInactive());

        // agora aciona o outro ramo (erro)
        assertThrows(BusinessRuleViolationException.class, inactive::inactivate);
    }
}
