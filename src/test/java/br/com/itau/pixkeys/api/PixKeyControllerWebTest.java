package br.com.itau.pixkeys.api;

import br.com.itau.pixkeys.api.dto.CreatePixKeyRequest;
import br.com.itau.pixkeys.domain.AccountType;
import br.com.itau.pixkeys.domain.KeyType;
import br.com.itau.pixkeys.validation.KeyValidator;
import br.com.itau.pixkeys.validation.SimpleKeyValidatorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PixKeyController.class)
@Import(ApiExceptionHandler.class)
class PixKeyControllerWebTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    SimpleKeyValidatorFactory factory;
    @MockitoBean
    KeyValidator emailValidator;

    private CreatePixKeyRequest reqOk() {
        return new CreatePixKeyRequest(
                KeyType.EMAIL, "ana@exemplo.com",
                AccountType.CHECKING, "1234", "00001234",
                "Ana", "Silva"
        );
    }

    @Test
    void post_shouldReturn204_whenValid() throws Exception {
        when(factory.get(KeyType.EMAIL)).thenReturn(emailValidator);
        doNothing().when(emailValidator).validate("ana@exemplo.com");

        mvc.perform(post("/pix-keys")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(reqOk())))
                .andExpect(status().isNoContent());

    }

    @Test
    void post_shouldReturn400_withFields_whenDtoInvalid() throws Exception {
        var bad = new CreatePixKeyRequest(null, " ", null, "12", "x", " ", null);

        mvc.perform(post("/pix-keys")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"))
                .andExpect(jsonPath("$.fields.keyType").exists())
                .andExpect(jsonPath("$.fields.keyValue").exists())
                .andExpect(jsonPath("$.fields.accountType").exists());
    }

    @Test
    void post_shouldReturn422_withDetail_whenDomainValidationFails() throws Exception {
        when(factory.get(KeyType.EMAIL)).thenReturn(emailValidator);
        doThrow(new IllegalArgumentException("email inválido"))
                .when(emailValidator).validate(anyString());

        mvc.perform(post("/pix-keys")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(reqOk())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de negócio inválida"))
                .andExpect(jsonPath("$.detail", containsString("inválido")));
    }
}
