package br.com.itau.pixkeys.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyStatusJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @EnumSource(KeyStatus.class)
    @ParameterizedTest(name = "round-trip JSON para {0}")
    void json_roundTrip_forAllConstants(KeyStatus s) throws Exception {
        String json = mapper.writeValueAsString(s);
        KeyStatus back = mapper.readValue(json, KeyStatus.class);
        assertEquals(s, back);
    }
}
