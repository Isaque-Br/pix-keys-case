package br.com.itau.pixkeys.api.dto;

import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreatePixKeyRequestJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serialize_shouldUsePtBrForAccountType_andKeepEnumNamesForKeyType() throws Exception {
        var req = new CreatePixKeyRequest(
                KeyType.EMAIL,
                "ana.silva+pix@exemplo.com",
                AccountType.CHECKING,
                "1234",
                "00001234",
                "Ana",
                "Silva"
        );

        String json = mapper.writeValueAsString(req);

        assertTrue(json.contains("\"keyType\":\"EMAIL\""), "Enum KeyType sai pelo nome");
        assertTrue(json.contains("\"keyValue\":\"ana.silva+pix@exemplo.com\""));
        assertTrue(json.contains("\"accountType\":\"corrente\""), "@JsonValue do AccountType em pt-BR");
        assertTrue(json.contains("\"agency\":\"1234\""));
        assertTrue(json.contains("\"account\":\"00001234\""));
        assertTrue(json.contains("\"holderName\":\"Ana\""));
        assertTrue(json.contains("\"holderSurname\":\"Silva\""));
    }

    @Test
    void deserialize_shouldAcceptAccountTypeWithAccent() throws Exception {
        String json = """
        {
          "keyType": "EMAIL",
          "keyValue": "ana@exemplo.com",
          "accountType": "poupança",
          "agency": "1234",
          "account": "00001234",
          "holderName": "Ana",
          "holderSurname": "Silva"
        }
        """;

        var req = mapper.readValue(json, CreatePixKeyRequest.class);

        assertEquals(KeyType.EMAIL, req.keyType());
        assertEquals(AccountType.SAVINGS, req.accountType());
        assertEquals("1234", req.agency());
        assertEquals("00001234", req.account());
        assertEquals("Ana", req.holderName());
        assertEquals("Silva", req.holderSurname());
    }
}
