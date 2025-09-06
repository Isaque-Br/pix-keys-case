package br.com.itau.pixkeys.domain;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
