package br.com.itau.pixkeys.domain;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AccountTypeJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serialize_shouldUsePtBrValues() throws Exception {
        assertEquals("\"corrente\"", mapper.writeValueAsString(AccountType.CHECKING));
        assertEquals("\"poupanca\"", mapper.writeValueAsString(AccountType.SAVINGS));
    }

    @Test
    void deserialize_shouldAcceptVariants() throws Exception {
        assertEquals(AccountType.CHECKING, mapper.readValue("\"CORRENTE\"", AccountType.class));
        assertEquals(AccountType.SAVINGS,  mapper.readValue("\"poupança\"", AccountType.class));
        assertEquals(AccountType.SAVINGS,  mapper.readValue("\"poupanca\"", AccountType.class));
    }

    @Test
    void deserialize_shouldFail_onUnknownValue() {
        var ex = assertThrows(ValueInstantiationException.class,
                () -> mapper.readValue("\"investimento\"", AccountType.class));

        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().toLowerCase().contains("inválido"));
    }
}
