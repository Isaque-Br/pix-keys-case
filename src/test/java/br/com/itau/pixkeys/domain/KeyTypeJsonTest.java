package br.com.itau.pixkeys.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyTypeJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @EnumSource(KeyType.class)
    @ParameterizedTest(name = "round-trip JSON para {0}")
    void json_roundTrip_forAllConstants(KeyType t) throws Exception {
        String json = mapper.writeValueAsString(t);
        KeyType back = mapper.readValue(json, KeyType.class);
        assertEquals(t, back);
    }
}
